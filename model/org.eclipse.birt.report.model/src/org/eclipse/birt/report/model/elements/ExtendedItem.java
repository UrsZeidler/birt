/*******************************************************************************
 * Copyright (c) 2004 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/

package org.eclipse.birt.report.model.elements;

import java.util.Collections;
import java.util.List;

import org.eclipse.birt.report.model.api.DesignElementHandle;
import org.eclipse.birt.report.model.api.ExtendedItemHandle;
import org.eclipse.birt.report.model.api.elements.ReportDesignConstants;
import org.eclipse.birt.report.model.api.extension.ExtendedElementException;
import org.eclipse.birt.report.model.api.extension.ICompatibleReportItem;
import org.eclipse.birt.report.model.api.extension.IPropertyDefinition;
import org.eclipse.birt.report.model.api.extension.IReportItem;
import org.eclipse.birt.report.model.api.metadata.IElementDefn;
import org.eclipse.birt.report.model.api.validators.ExtensionValidator;
import org.eclipse.birt.report.model.core.ContainerSlot;
import org.eclipse.birt.report.model.core.DesignElement;
import org.eclipse.birt.report.model.core.Module;
import org.eclipse.birt.report.model.core.PropertySearchStrategy;
import org.eclipse.birt.report.model.elements.interfaces.IExtendedItemModel;
import org.eclipse.birt.report.model.elements.strategy.ExtendedItemPropSearchStrategy;
import org.eclipse.birt.report.model.extension.DummyPeerExtensibilityProvider;
import org.eclipse.birt.report.model.extension.IExtendableElement;
import org.eclipse.birt.report.model.extension.PeerExtensibilityProvider;
import org.eclipse.birt.report.model.extension.PeerExtensibilityProviderFactory;
import org.eclipse.birt.report.model.metadata.ElementPropertyDefn;
import org.eclipse.birt.report.model.metadata.ExtensionElementDefn;
import org.eclipse.birt.report.model.util.ContentIterator;

/**
 * This class represents an extended item element. The extended report item
 * allows third-party developers to create report items that work within BIRT
 * virtually identically to BIRT-defined items. Extended items can use the
 * user-properties discussed above to define properties, can use a
 * ��black-box�� approach, or a combination of the two. Extended items
 * are defined in a Java plug-in that contributes behavior to the Eclipse Report
 * Developer, to the Factory and to the Presentation Engine. The extended item
 * can fully participate with the other BIRT extension facilities, meaning that
 * report developers can additional properties and scripts to an extended item,
 * providing a very powerful way to create application-specific functionality.
 * An extended item is defined by a plug-in. The plug-in is specific to BIRT,
 * and is different from an Eclipse plug-in. Each item plug-in has four parts:
 * <ul>
 * <li>Design: handles the model that describes the report item.
 * <li>User Interface: the UI displayed for the item. This is in the form of an
 * Eclipse plug-in.
 * <li>Factory: how to gather the data for the extended item, and compute its
 * default size in the Factory.
 * <li>Presentation: how to render the extended item when rendering the report
 * to HTML, PDF or other formats.
 * </ul>
 * 
 * 
 */

public class ExtendedItem extends ReportItem
		implements
			IExtendableElement,
			IExtendedItemModel
{

	/**
	 * Extended item can support extension. It has a unique name to identify the
	 * extension. Using this name, BIRT can get the extension definition. The
	 * name is an internal name for an implementation of extension.
	 * <p>
	 * The name does not occur in a name space.
	 */

	protected String extensionName = null;

	/**
	 * The version of the extended element model. It is individual from the
	 * version of the report design.
	 */

	protected String extensionVersion = null;

	/**
	 * The extensibility provider which provides the functionality of this
	 * extensible element.
	 */

	protected PeerExtensibilityProvider provider = null;

	/**
	 * The methods for the extension element.
	 */

	private List methods = null;

	/**
	 * PropertDefinitions of the extension element.
	 */

	private List props = null;

	/**
	 * Default constructor.
	 */

	public ExtendedItem( )
	{
		provider = new DummyPeerExtensibilityProvider( this, null );
	}

	/**
	 * Constructs the extended item with an optional name.
	 * 
	 * @param theName
	 *            optional item name
	 */

	public ExtendedItem( String theName )
	{
		super( theName );
		provider = new DummyPeerExtensibilityProvider( this, null );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.model.core.DesignElement#apply(org.eclipse.birt.report.model.elements.ElementVisitor)
	 */

	public void apply( ElementVisitor visitor )
	{
		visitor.visitExtendedItem( this );

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.model.core.DesignElement#getElementName()
	 */

	public String getElementName( )
	{
		return ReportDesignConstants.EXTENDED_ITEM;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.model.core.DesignElement#getHandle(org.eclipse.birt.report.model.elements.ReportDesign)
	 */

	public DesignElementHandle getHandle( Module module )
	{
		return handle( module );
	}

	/**
	 * Returns an API handle for this element.
	 * 
	 * @param module
	 *            the report design
	 * 
	 * @return an API handle for this element.
	 */

	public ExtendedItemHandle handle( Module module )
	{
		if ( handle == null )
		{
			handle = new ExtendedItemHandle( module, this );
		}
		return (ExtendedItemHandle) handle;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.model.core.DesignElement#hasLocalPropertyValues()
	 */

	public boolean hasLocalPropertyValues( )
	{
		if ( super.hasLocalPropertyValues( ) )
			return true;

		return provider.hasLocalPropertyValues( );
	}

	/**
	 * Gets a property value given its definition. This version checks not only
	 * this one object, but also the extended element this item has. That is, it
	 * gets the "local" property value. The property name must also be valid for
	 * this object.
	 * 
	 * @see org.eclipse.birt.report.model.core.DesignElement#getLocalProperty(Module,
	 *      org.eclipse.birt.report.model.metadata.ElementPropertyDefn)
	 */

	public Object getLocalProperty( Module module, ElementPropertyDefn prop )
	{
		assert prop != null;

		if ( !prop.isExtended( ) )
			return super.getLocalProperty( module, prop );

		return provider.getExtensionProperty( prop.getName( ) );
	}

	/**
	 * Sets the value of a property. The value must have already been validated,
	 * and must be of the correct type for the property. The property must be
	 * valid for this object. The property can be a system, user-defined
	 * property and a property from the extended element of this item. The value
	 * is set locally. If the value is null, then the property is "unset."
	 * 
	 * @see org.eclipse.birt.report.model.core.DesignElement#setProperty(org.eclipse.birt.report.model.metadata.ElementPropertyDefn,
	 *      java.lang.Object)
	 */

	public void setProperty( ElementPropertyDefn prop, Object value )
	{
		assert prop != null;

		if ( !prop.isExtended( ) )
			super.setProperty( prop, value );
		else
			provider.setExtensionProperty( prop, value );
	}

	/**
	 * Gets the property data for either a system-defined, user-defined property
	 * or extension property from extended element of this item.
	 * 
	 * @see org.eclipse.birt.report.model.core.DesignElement#getPropertyDefn(java.lang.String)
	 */

	public ElementPropertyDefn getPropertyDefn( String propName )
	{
		assert propName != null;

		ElementPropertyDefn propDefn = super.getPropertyDefn( propName );
		if ( propDefn != null )
			return propDefn;

		return (ElementPropertyDefn) provider.getPropertyDefn( propName );
	}

	/**
	 * Gets the list of property definitions available to this element. Includes
	 * all properties defined for this element, all user-defined properties
	 * defined on this element or its ancestors, any style properties that this
	 * element supports and extension properties that the extended element of
	 * this item supports.
	 * 
	 * @see org.eclipse.birt.report.model.core.DesignElement#getPropertyDefns()
	 */

	public List getPropertyDefns( )
	{
		if ( props == null )
			props = provider.getPropertyDefns( );

		return props;
	}

	/**
	 * Gets the definition of the extension element.
	 * 
	 * @return the definition of the extension element if found, or null if the
	 *         extended item is not extensible or the extension element is not
	 *         registered in BIRT
	 */

	public ExtensionElementDefn getExtDefn( )
	{
		return provider.getExtDefn( );
	}

	/**
	 * Creates an instance of <code>IReportItem</code> to store the
	 * information of the peer extension. When the application invokes UI for
	 * the extended item, it calls this method to get the instance of the peer
	 * extension and reads the information--property values from the BIRT ROM
	 * properties. If there is no instance of peer for the item before the
	 * calling and then it is successfully created. If the item has no extension
	 * peer for it or the peer instance has been created before, then there is
	 * no operation.
	 * 
	 * @param module
	 *            the module the peer element has
	 * @throws ExtendedElementException
	 *             if the serialized model is invalid
	 */

	public void initializeReportItem( Module module )
			throws ExtendedElementException
	{
		provider.initializeReportItem( module );
	}

	/**
	 * Returns the extensibility provider which provides the functionality of
	 * this extensible element.
	 * 
	 * @return the extensibility provider which provides the functionality of
	 *         this extensible element.
	 */

	public PeerExtensibilityProvider getExtensibilityProvider( )
	{
		return provider;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.model.core.DesignElement#getIntrinsicProperty(java.lang.String)
	 */

	protected Object getIntrinsicProperty( String propName )
	{
		if ( EXTENSION_NAME_PROP.equals( propName ) )
			return extensionName;
		if ( EXTENSION_VERSION_PROP.equals( propName ) )
			return extensionVersion;
		return super.getIntrinsicProperty( propName );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.model.core.DesignElement#setIntrinsicProperty(java.lang.String,
	 *      java.lang.Object)
	 */

	protected void setIntrinsicProperty( String propName, Object value )
	{
		if ( EXTENSION_NAME_PROP.equals( propName ) )
		{
			setExtensionName( (String) value );
		}
		else if ( EXTENSION_VERSION_PROP.equals( propName ) )
		{
			this.extensionVersion = (String) value;
		}
		else
		{
			super.setIntrinsicProperty( propName, value );
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.model.core.DesignElement#validate(org.eclipse.birt.report.model.elements.ReportDesign)
	 */

	public List validate( Module module )
	{
		List list = super.validate( module );

		list
				.addAll( ExtensionValidator.getInstance( ).validate( module,
						this ) );

		return list;
	}

	/**
	 * Gets the effective extension element of this extended item.
	 * 
	 * @return the effective extension element
	 */

	public IReportItem getExtendedElement( )
	{
		return provider.getExtensionElement( );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.model.core.DesignElement#baseClone()
	 */
	protected Object baseClone( ) throws CloneNotSupportedException
	{
		ExtendedItem clonedElement = (ExtendedItem) super.baseClone( );

		clonedElement.provider = PeerExtensibilityProviderFactory
				.createProvider( clonedElement, clonedElement.extensionName );
		clonedElement.provider.copyFrom( provider );

		return clonedElement;
	}

	/**
	 * Tests whether the property is a dynamic property of extended element or
	 * not.
	 * 
	 * @param propName
	 *            the property name to check
	 * @return true if the property is one of the dynamic properties for
	 *         extended element, otherwise false
	 */

	public boolean isExtensionModelProperty( String propName )
	{
		return provider.isExtensionModelProperty( propName );
	}

	/**
	 * Tests whether the property is just the model property for the extended
	 * element, and its type is XML.
	 * 
	 * @param propName
	 *            the property name to check
	 * @return true if the property is XML type and it is model property for
	 *         extended element, otherwise false
	 */

	public boolean isExtensionXMLProperty( String propName )
	{
		return provider.isExtensionXMLProperty( propName );
	}

	/**
	 * returns the methods defined on this element and defined in the extension
	 * model.
	 * 
	 * @return the method list
	 */
	public List getMethods( )
	{
		if ( methods == null )
			methods = provider.getModelMethodDefns( );

		return methods;
	}

	/**
	 * Returns the script property name of this extended item.
	 * 
	 * @return the script property name
	 */

	public String getScriptPropertyName( )
	{
		IPropertyDefinition defn = provider.getScriptPropertyDefinition( );
		return defn == null ? null : defn.getName( );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.model.core.DesignElement#getStrategy()
	 */

	public PropertySearchStrategy getStrategy( )
	{
		return ExtendedItemPropSearchStrategy.getInstance( );
	}

	/**
	 * Sets the extension name for this extended item. At the same time,
	 * initialize the extension provider and slot.
	 * 
	 * @param extension
	 *            the extension name to set
	 */

	private void setExtensionName( String extension )
	{
		extensionName = extension;
		provider = PeerExtensibilityProviderFactory.createProvider( this,
				extensionName );
		initSlots( );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.model.core.DesignElement#getSlot(int)
	 */

	public ContainerSlot getSlot( int slot )
	{
		assert slot >= 0 && slot < getDefn( ).getSlotCount( );
		return slots[slot];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.model.core.DesignElement#getDefn()
	 */
	public IElementDefn getDefn( )
	{
		IElementDefn extDefn = getExtDefn( );
		if ( extDefn != null )
			return extDefn;
		return super.getDefn( );
	}

	/**
	 * Gets the default element definition of this extended-item.
	 * 
	 * @return the default element definition
	 */

	public IElementDefn getDefaultDefn( )
	{
		return super.getDefn( );
	}

	/**
	 * Returns if this extended item has local property values on own model.
	 * 
	 * @return <code>true</code> if this extended item has local property
	 *         values on own model, <code>false</code> otherwise.
	 */

	public boolean hasLocalPropertyValuesOnOwnModel( )
	{
		return provider.hasLocalPropertyValuesOnOwnModel( );
	}

	/**
	 * Returns the predefined styles that are provided by the
	 * ExtendedItem.reportItem.
	 * 
	 * @param module
	 *            the root of the extended item
	 * @return a list containing predefined selectors in string
	 */

	public List getReportItemDefinedSelectors( Module module )
	{
		IReportItem reportItem = getExtendedElement( );
		try
		{
			if ( reportItem == null )
			{
				initializeReportItem( module );
				reportItem = getExtendedElement( );
			}
		}
		catch ( ExtendedElementException e )
		{
		}
		if ( reportItem == null )
			return Collections.EMPTY_LIST;

		return reportItem.getPredefinedStyles( );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.model.core.DesignElement#getLocalEncryptionID(org.eclipse.birt.report.model.metadata.ElementPropertyDefn)
	 */
	public String getLocalEncryptionID( ElementPropertyDefn propDefn )
	{
		if ( propDefn == null || !propDefn.isEncryptable( ) )
			return null;
		if ( !propDefn.isExtended( ) )
			return super.getLocalEncryptionID( propDefn );

		return provider.getEncryptionHelperID( propDefn );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.model.core.DesignElement#setEncryptionHelper(org.eclipse.birt.report.model.metadata.ElementPropertyDefn,
	 *      java.lang.String)
	 */
	public void setEncryptionHelper( ElementPropertyDefn propDefn,
			String encryptionID )
	{
		if ( !propDefn.isExtended( ) )
			super.setEncryptionHelper( propDefn, encryptionID );

		provider.setEncryptionHelper( propDefn, encryptionID );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.model.core.DesignElement#hasLocalValue(org.eclipse.birt.report.model.metadata.ElementPropertyDefn)
	 */
	protected boolean hasLocalValue( ElementPropertyDefn propDefn )
	{
		if ( propDefn == null )
			return false;
		if ( !propDefn.isExtended( ) )
			return super.hasLocalValue( propDefn );
		return provider.getExtensionProperty( propDefn.getName( ) ) != null;
	}

	/**
	 * Checks the compatibilities for this extended item.
	 * 
	 * @param module
	 * @return
	 */
	public List checkCompatibility( Module module )
	{
		// check this element itself
		List errorList = doCheck( module );

		// if this extended-item has parent, then check all virtual children
		if ( getExtendsElement( ) != null )
		{
			ContentIterator iter = new ContentIterator( module, this );
			while ( iter.hasNext( ) )
			{
				DesignElement content = (DesignElement) iter.next( );
				if ( content instanceof ExtendedItem )
				{
					List error = ( (ExtendedItem) content ).doCheck( module );
					errorList.addAll( error );
				}
			}
		}

		return errorList;
	}

	private List doCheck( Module module )
	{
		if ( module != null && module.isReadOnly( ) )
			return Collections.EMPTY_LIST;

		if ( !provider.needCheckCompatibility( ) )
			return Collections.EMPTY_LIST;
		try
		{
			initializeReportItem( module );
		}
		catch ( ExtendedElementException e )
		{
			return Collections.EMPTY_LIST;
		}
		IReportItem item = getExtendedElement( );

		if ( item instanceof ICompatibleReportItem )
		{
			List error = ( (ICompatibleReportItem) item ).checkCompatibility( );
			return error;
		}

		return Collections.EMPTY_LIST;
	}
}