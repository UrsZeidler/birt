package org.eclipse.birt.report.engine.executor;

import org.eclipse.birt.report.engine.content.IContent;
import org.eclipse.birt.report.engine.content.ITableGroupContent;
import org.eclipse.birt.report.engine.ir.TableGroupDesign;


public class TableGroupExecutor extends GroupExecutor
{
	protected TableGroupExecutor( ExecutorManager manager )
	{
		super( manager );
	}

	public void close( )
	{
		handlePageBreakAfterExclusingLast( );
		ITableGroupContent groupContent = (ITableGroupContent) getContent( );
		if ( emitter != null )
		{
			emitter.endTableGroup( groupContent );
		}
		context.unregisterOnPageBreak( content );
		finishGroupTOCEntry( );
		manager.releaseExecutor( ExecutorManager.TABLEGROUPITEM, this );
	}

	public IContent execute( )
	{
		TableGroupDesign groupDesign = ( TableGroupDesign ) getDesign();

		ITableGroupContent groupContent = report.createTableGroupContent( );
		setContent(groupContent);
		
		restoreResultSet( );
		context.registerOnPageBreak( content );
		
		initializeContent( groupDesign, groupContent );
		processBookmark( groupDesign, groupContent );
		handlePageBreakBeforeOfGroup( );
		handlePageBreakAfterOfGroup( );
		handlePageBreakAfterOfPreviousGroup( );

		startGroupTOCEntry( groupContent );
		if ( emitter != null )
		{
			emitter.startTableGroup( groupContent );
		}

		//prepare to execute the children
		prepareToExecuteChildren();
		
		return groupContent;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.birt.report.engine.executor.GroupExecutor#getNextChild()
	 */
	public IReportItemExecutor getNextChild( )
	{
		IReportItemExecutor executor = super.getNextChild( );
		if ( executor instanceof TableBandExecutor )
		{
			TableBandExecutor bandExecutor = (TableBandExecutor) executor;
			bandExecutor.setTableExecutor( (TableItemExecutor) listingExecutor );
		}
		return executor;
	}
}