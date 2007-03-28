/*
 *************************************************************************
 * Copyright (c) 2006 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *  
 *************************************************************************
 */ 
package org.eclipse.birt.report.data.adapter.internal.adapter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.birt.core.data.ExpressionUtil;
import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.data.engine.api.IColumnDefinition;
import org.eclipse.birt.data.engine.api.querydefn.BaseDataSetDesign;
import org.eclipse.birt.data.engine.api.querydefn.BaseDataSourceDesign;
import org.eclipse.birt.data.engine.api.querydefn.ColumnDefinition;
import org.eclipse.birt.report.data.adapter.api.AdapterException;
import org.eclipse.birt.report.data.adapter.i18n.ResourceConstants;
import org.eclipse.birt.report.model.api.ColumnHintHandle;
import org.eclipse.birt.report.model.api.ComputedColumnHandle;
import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.DataSetParameterHandle;
import org.eclipse.birt.report.model.api.DataSourceHandle;
import org.eclipse.birt.report.model.api.FilterConditionHandle;
import org.eclipse.birt.report.model.api.JointDataSetHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.eclipse.birt.report.model.api.OdaDataSetParameterHandle;
import org.eclipse.birt.report.model.api.OdaResultSetColumnHandle;
import org.eclipse.birt.report.model.api.ParamBindingHandle;
import org.eclipse.birt.report.model.api.ReportElementHandle;
import org.eclipse.birt.report.model.api.ResultSetColumnHandle;
import org.eclipse.birt.report.model.api.elements.DesignChoiceConstants;
import org.eclipse.birt.report.model.api.metadata.IPropertyDefn;

/**
 * Utility class for data adaptors
 */
class DataAdapterUtil
{
	/**
	 * Adapts common base data source properties
	 */
	public static void adaptBaseDataSource( DataSourceHandle source, BaseDataSourceDesign dest )
	{
		dest.setBeforeOpenScript( source.getBeforeOpen( ) );
		dest.setAfterOpenScript( source.getAfterOpen( ) );
		dest.setBeforeCloseScript( source.getBeforeClose( ) );
		dest.setAfterCloseScript( source.getAfterClose( ) );
	}

	/**
	 * Adapts base data set properties
	 */
	public static void adaptBaseDataSet( DataSetHandle modelDataSet,
			BaseDataSetDesign dteDataSet ) throws BirtException
	{
		if ( ( !( modelDataSet instanceof JointDataSetHandle ) )
				&& modelDataSet.getDataSource( ) == null )
		{
			throw new AdapterException( ResourceConstants.DATASOURCE_NULL_ERROR );
		}
		
		if ( !( modelDataSet instanceof JointDataSetHandle ) )
		{
			dteDataSet.setDataSource( modelDataSet.getDataSource( )
					.getQualifiedName( ) );
			dteDataSet.setBeforeOpenScript( modelDataSet.getBeforeOpen( ) );
			dteDataSet.setAfterOpenScript( modelDataSet.getAfterOpen( ) );
			dteDataSet.setOnFetchScript( modelDataSet.getOnFetch( ) );
			dteDataSet.setBeforeCloseScript( modelDataSet.getBeforeClose( ) );
			dteDataSet.setAfterCloseScript( modelDataSet.getAfterClose( ) );
			
			// cache row count
			dteDataSet.setCacheRowCount( modelDataSet.getCachedRowCount( ) );

		}
		populateParameter( modelDataSet, dteDataSet );
		
		populateComputedColumn( modelDataSet, dteDataSet );

		populateFilter( modelDataSet, dteDataSet );

		dteDataSet.setRowFetchLimit( modelDataSet.getRowFetchLimit( ) );
		
		mergeHints( modelDataSet, dteDataSet );

	}

	/**
	 * 
	 * @param modelDataSet
	 * @param dteDataSet
	 */
	private static void populateParameter( DataSetHandle modelDataSet, BaseDataSetDesign dteDataSet )
	{
		//dataset parameters definition
		HashMap paramBindingCandidates = new HashMap( );

		Iterator elmtIter = modelDataSet.parametersIterator( );
		if ( elmtIter != null )
		{
			while ( elmtIter.hasNext( ) )
			{
				DataSetParameterHandle modelParam = ( DataSetParameterHandle ) elmtIter
						.next( );

				if( modelParam.getDefaultValue( )== null )
					continue;
				
				dteDataSet.addParameter( new ParameterAdapter( modelParam ) );
				// collect input parameter default values as
				// potential parameter binding if no explicit ones are
				// defined for a parameter
				if ( modelParam.isInput( ) )
				{
					String defaultValueExpr = null;
					if ( modelParam instanceof OdaDataSetParameterHandle
							&& ( (OdaDataSetParameterHandle) modelParam ).getParamName( ) != null )
					{
						defaultValueExpr = ExpressionUtil.createJSParameterExpression( ( ( (OdaDataSetParameterHandle) modelParam ).getParamName( ) ) );
					}
					else
						defaultValueExpr = modelParam.getDefaultValue( );
					if ( defaultValueExpr != null )
						paramBindingCandidates.put( modelParam.getName( ),
								defaultValueExpr );
				}
			}
		}

		// input parameter bindings
		elmtIter = modelDataSet.paramBindingsIterator( );
		if ( elmtIter != null )
		{
			while ( elmtIter.hasNext( ) )
			{
				ParamBindingHandle modelParamBinding = ( ParamBindingHandle ) elmtIter
						.next( );
				// replace default value of the same parameter, if defined
				paramBindingCandidates.put( modelParamBinding.getParamName( ),
						modelParamBinding.getExpression( ) );
			}
		}

		// assign merged parameter bindings to the data set
		if ( paramBindingCandidates.size( ) > 0 )
		{
			elmtIter = paramBindingCandidates.keySet( ).iterator( );
			while ( elmtIter.hasNext( ) )
			{
				Object paramName = elmtIter.next( );
				assert ( paramName != null && paramName instanceof String );
				String expression = ( String ) paramBindingCandidates
						.get( paramName );
				dteDataSet.addInputParamBinding( new InputParamBindingAdapter(
						( String ) paramName, expression ) );
			}
		}
	}
	
	/**
	 * 
	 * @param modelDataSet
	 * @param dteDataSet
	 */
	private static void populateComputedColumn( DataSetHandle modelDataSet, BaseDataSetDesign dteDataSet )
	{
		// computed columns
		Iterator elmtIter = modelDataSet.computedColumnsIterator( );
		if ( elmtIter != null )
		{
			while ( elmtIter.hasNext( ) )
			{
				ComputedColumnHandle modelCmptdColumn = ( ComputedColumnHandle ) elmtIter
						.next( );
				dteDataSet.addComputedColumn( new ComputedColumnAdapter( modelCmptdColumn ));
			}
		}
	}
	
	/**
	 * 
	 * @param modelDataSet
	 * @param dteDataSet
	 */
	private static void populateFilter( DataSetHandle modelDataSet, BaseDataSetDesign dteDataSet )
	{
		// filter conditions
		Iterator elmtIter = modelDataSet.filtersIterator( );
		if ( elmtIter != null )
		{
			while ( elmtIter.hasNext( ) )
			{
				FilterConditionHandle modelFilter = ( FilterConditionHandle ) elmtIter
						.next( );
				dteDataSet.addFilter( new FilterAdapter( modelFilter ) );
			}
		}
	}
	
	/**
	 * 
	 * @param modelDataSet
	 * @param dteDataSet
	 */
	private static void mergeHints( DataSetHandle modelDataSet, BaseDataSetDesign dteDataSet )
	{
		// merge ResultSetHints and ColumnHints, the order is important.
		// ResultSetHints will give each column a unique name, and
		// column hints should base on the result of ResultSet hint.
		// So in ResultSetHint list, the order of items should be
		// ResultSetColumn and then ColumnHint.

		// now merge model's result set column info into existing columnDefn
		// with same column name, otherwise create new columnDefn
		// based on the model's result set column
		Iterator elmtIter;
		if ( modelDataSet instanceof OdaDataSetHandle )
		{
			elmtIter = modelDataSet.resultSetIterator( );
			if ( elmtIter != null )
			{
				while ( elmtIter.hasNext( ) )
				{
					OdaResultSetColumnHandle modelColumn = (OdaResultSetColumnHandle) elmtIter.next( );
					if ( !modelColumn.getColumnName( )
							.equals( modelColumn.getNativeName( ) ) )
						dteDataSet.addResultSetHint( new ColumnAdapter( (ResultSetColumnHandle) modelColumn ) );
				}
			}
		}
		
		elmtIter = modelDataSet.resultSetHintsIterator( );
		if ( elmtIter != null )
		{
			while ( elmtIter.hasNext( ) )
			{
				ResultSetColumnHandle modelColumn = (ResultSetColumnHandle) elmtIter.next( );
				dteDataSet.addResultSetHint( new ColumnAdapter( modelColumn ) );
			}
		}

		// merging result set column and column hints into DtE columnDefn;
		// first create new columnDefn based on model's column hints
		elmtIter = modelDataSet.columnHintsIterator( );
		if ( elmtIter != null )
		{
			List columnDefns = dteDataSet.getResultSetHints( );
			while ( elmtIter.hasNext( ) )
			{
				ColumnHintHandle modelColumnHint = ( ColumnHintHandle ) elmtIter
						.next( );
				ColumnDefinition existDefn = findColumnDefn( columnDefns,
						modelColumnHint.getColumnName( ) );
				if ( existDefn != null )
					updateColumnDefn( existDefn, modelColumnHint );
				else
					dteDataSet
							.addResultSetHint( new ColumnAdapter( modelColumnHint ) );
			}
		}
	}
	
	public static void updateColumnDefn( ColumnDefinition dteColumn,
			ColumnHintHandle modelColumnHint )
	{
		assert dteColumn.getColumnName( ).equals(
				modelColumnHint.getColumnName( ) );
		dteColumn.setAlias( modelColumnHint.getAlias( ) );

		String exportConstant = modelColumnHint.getExport( );
		if ( exportConstant != null )
		{
			int exportHint = IColumnDefinition.DONOT_EXPORT; // default value
			if ( exportConstant
					.equals( DesignChoiceConstants.EXPORT_TYPE_IF_REALIZED ) )
				exportHint = IColumnDefinition.EXPORT_IF_REALIZED;
			else if ( exportConstant
					.equals( DesignChoiceConstants.EXPORT_TYPE_ALWAYS ) )
				exportHint = IColumnDefinition.ALWAYS_EXPORT;
			else
				assert exportConstant
						.equals( DesignChoiceConstants.EXPORT_TYPE_NONE );

			dteColumn.setExportHint( exportHint );
		}

		String searchConstant = modelColumnHint.getSearching( );
		if ( searchConstant != null )
		{
			int searchHint = IColumnDefinition.NOT_SEARCHABLE;
			if ( searchConstant
					.equals( DesignChoiceConstants.SEARCH_TYPE_INDEXED ) )
				searchHint = IColumnDefinition.SEARCHABLE_IF_INDEXED;
			else if ( searchConstant
					.equals( DesignChoiceConstants.SEARCH_TYPE_ANY ) )
				searchHint = IColumnDefinition.ALWAYS_SEARCHABLE;
			else
				assert searchConstant
						.equals( DesignChoiceConstants.SEARCH_TYPE_NONE );

			dteColumn.setSearchHint( searchHint );
		}

	}
	
	/**
	 * Find the DtE columnDefn from the given list of columnDefns that matches
	 * the given columnName.
	 */
	private static ColumnDefinition findColumnDefn( List columnDefns, String columnName )
	{
		assert columnName != null;
		if ( columnDefns == null )
			return null; // no list to find from
		Iterator iter = columnDefns.iterator( );
		if ( iter == null )
			return null;

		// iterate thru each columnDefn, and looks for a match of
		// specified column name
		while ( iter.hasNext( ) )
		{
			ColumnDefinition column = ( ColumnDefinition ) iter.next( );
			if ( columnName.equals( column.getColumnName( ) ) )
				return column;
		}
		return null;
	}

	/**
	 * Gets the data handle's static ROM extension properties name and value
	 * pairs in String values and returns them in a Map
	 */
	public static Map getExtensionProperties( ReportElementHandle dataHandle,
			List driverPropList )
	{
		if ( driverPropList == null || driverPropList.isEmpty( ) )
			return null; // nothing to add

		Map properties = new HashMap( );
		Iterator elmtIter = driverPropList.iterator( );
		while ( elmtIter.hasNext( ) )
		{
			IPropertyDefn modelExtProp = ( IPropertyDefn ) elmtIter.next( );

			// First get extension property's name
			String propName = modelExtProp.getName( );
			assert ( propName != null && propName.length( ) > 0 );

			// Use property name to get property value
			Object propValueObj = dataHandle.getProperty( modelExtProp
					.getName( ) );

			/*
			 * An ODA consumer does not distinguish whether a property value is
			 * not set or explicitly set to null. Its handling is pushed down to
			 * the underlying data provider.
			 */
			String propValue = ( propValueObj == null ) ? null : propValueObj
					.toString( );
			properties.put( propName, propValue );
		}

		return properties;
	}

}
