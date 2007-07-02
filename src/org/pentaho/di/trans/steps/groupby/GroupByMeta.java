 /**********************************************************************
 **                                                                   **
 **               This code belongs to the KETTLE project.            **
 **                                                                   **
 ** Kettle, from version 2.2 on, is released into the public domain   **
 ** under the Lesser GNU Public License (LGPL).                       **
 **                                                                   **
 ** For more details, please read the document LICENSE.txt, included  **
 ** in this project                                                   **
 **                                                                   **
 ** http://www.kettle.be                                              **
 ** info@kettle.be                                                    **
 **                                                                   **
 **********************************************************************/

package org.pentaho.di.trans.steps.groupby;

import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.Counter;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.w3c.dom.Node;


/**
 * Created on 02-jun-2003
 *
 */

public class GroupByMeta extends BaseStepMeta implements StepMetaInterface
{
	public static final int TYPE_GROUP_NONE            =  0;
	public static final int TYPE_GROUP_SUM             =  1;
	public static final int TYPE_GROUP_AVERAGE         =  2;
	public static final int TYPE_GROUP_MIN             =  3;
	public static final int TYPE_GROUP_MAX             =  4;
	public static final int TYPE_GROUP_COUNT_ALL       =  5;
    public static final int TYPE_GROUP_CONCAT_COMMA    =  6;
    public static final int TYPE_GROUP_FIRST           =  7;
    public static final int TYPE_GROUP_LAST            =  8;
    public static final int TYPE_GROUP_FIRST_INCL_NULL =  9;
    public static final int TYPE_GROUP_LAST_INCL_NULL  = 10;

	public static final String typeGroupCode[] =  /* WARNING: DO NOT TRANSLATE THIS. WE ARE SERIOUS, DON'T TRANSLATE! */ 
		{
			"-", "SUM", "AVERAGE", "MIN", "MAX", "COUNT_ALL", "CONCAT_COMMA", "FIRST", "LAST", "FIRST_INCL_NULL", "LAST_INCL_NULL",   	 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$
		};

	public static final String typeGroupLongDesc[] = 
		{
			"-",                                                                   //$NON-NLS-1$ 
            Messages.getString("GroupByMeta.TypeGroupLongDesc.SUM"),               //$NON-NLS-1$ 
            Messages.getString("GroupByMeta.TypeGroupLongDesc.AVERAGE"),           //$NON-NLS-1$ 
            Messages.getString("GroupByMeta.TypeGroupLongDesc.MIN"),               //$NON-NLS-1$ 
            Messages.getString("GroupByMeta.TypeGroupLongDesc.MAX"),               //$NON-NLS-1$ 
            Messages.getString("GroupByMeta.TypeGroupLongDesc.CONCAT_ALL"),        //$NON-NLS-1$ 
            Messages.getString("GroupByMeta.TypeGroupLongDesc.CONCAT_COMMA"),      //$NON-NLS-1$ 
            Messages.getString("GroupByMeta.TypeGroupLongDesc.FIRST"),             //$NON-NLS-1$ 
            Messages.getString("GroupByMeta.TypeGroupLongDesc.LAST"), 	           //$NON-NLS-1$ 
            Messages.getString("GroupByMeta.TypeGroupLongDesc.FIRST_INCL_NULL"),   //$NON-NLS-1$ 
            Messages.getString("GroupByMeta.TypeGroupLongDesc.LAST_INCL_NULL"),    //$NON-NLS-1$ 
		};

	
	
	/** All rows need to pass, adding an extra row at the end of each group/block. */
	private boolean passAllRows;
	
    /** Directory to store the temp files */
    private String  directory;
    /** Temp files prefix... */
    private String  prefix;
    
	/** Indicate that some rows don't need to be considered : TODO: make work in GUI & worker */
	private boolean aggregateIgnored; 
	/** name of the boolean field that indicates we need to ignore the row : TODO: make work in GUI & worker */
	private String  aggregateIgnoredField; 

	/** Fields to group over */
	private String  groupField[]; 

	/** Name of aggregate field */
	private String  aggregateField[]; 
	/** Field name to group over */
	private String  subjectField[]; 
	/** Type of aggregate */
	private int     aggregateType[]; 
	
    /** Add a linenr in the group, resetting to 0 in a new group. */
    private boolean addingLineNrInGroup;
    
    /** The fieldname that will contain the added integer field */
    private String lineNrInGroupField;
    
	public GroupByMeta()
	{
		super(); // allocate BaseStepMeta
	}
	
    /**
     * @return Returns the aggregateField.
     */
    public String[] getAggregateField()
    {
        return aggregateField;
    }
    
    /**
     * @param aggregateField The aggregateField to set.
     */
    public void setAggregateField(String[] aggregateField)
    {
        this.aggregateField = aggregateField;
    }
    
    /**
     * @return Returns the aggregateIgnored.
     */
    public boolean isAggregateIgnored()
    {
        return aggregateIgnored;
    }
    
    /**
     * @param aggregateIgnored The aggregateIgnored to set.
     */
    public void setAggregateIgnored(boolean aggregateIgnored)
    {
        this.aggregateIgnored = aggregateIgnored;
    }
    
    /**
     * @return Returns the aggregateIgnoredField.
     */
    public String getAggregateIgnoredField()
    {
        return aggregateIgnoredField;
    }
    
    /**
     * @param aggregateIgnoredField The aggregateIgnoredField to set.
     */
    public void setAggregateIgnoredField(String aggregateIgnoredField)
    {
        this.aggregateIgnoredField = aggregateIgnoredField;
    }
    
    /**
     * @return Returns the aggregateType.
     */
    public int[] getAggregateType()
    {
        return aggregateType;
    }
    
    /**
     * @param aggregateType The aggregateType to set.
     */
    public void setAggregateType(int[] aggregateType)
    {
        this.aggregateType = aggregateType;
    }
    
    /**
     * @return Returns the groupField.
     */
    public String[] getGroupField()
    {
        return groupField;
    }
    
    /**
     * @param groupField The groupField to set.
     */
    public void setGroupField(String[] groupField)
    {
        this.groupField = groupField;
    }
    
    /**
     * @return Returns the passAllRows.
     */
    public boolean passAllRows()
    {
        return passAllRows;
    }
    
    /**
     * @param passAllRows The passAllRows to set.
     */
    public void setPassAllRows(boolean passAllRows)
    {
        this.passAllRows = passAllRows;
    }
    
    /**
     * @return Returns the subjectField.
     */
    public String[] getSubjectField()
    {
        return subjectField;
    }
    
    /**
     * @param subjectField The subjectField to set.
     */
    public void setSubjectField(String[] subjectField)
    {
        this.subjectField = subjectField;
    }
    
    
    
	public void loadXML(Node stepnode, List<DatabaseMeta> databases, Map<String, Counter> counters)
		throws KettleXMLException
	{
		readData(stepnode);
	}

	public void allocate(int sizegroup, int nrfields)
	{
		groupField = new String[sizegroup];
		aggregateField  = new String[nrfields];
		subjectField = new String[nrfields];
		aggregateType  = new int[nrfields];
	}

	public Object clone()
	{
		Object retval = super.clone();
		return retval;
	}
	
	private void readData(Node stepnode)
		throws KettleXMLException
	{
		try
		{
			passAllRows    = "Y".equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "all_rows")); //$NON-NLS-1$ //$NON-NLS-2$
			aggregateIgnored = "Y".equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "ignore_aggregate")); //$NON-NLS-1$ //$NON-NLS-2$
			aggregateIgnoredField     = XMLHandler.getTagValue(stepnode, "field_ignore"); //$NON-NLS-1$
	
            directory = XMLHandler.getTagValue(stepnode, "directory"); //$NON-NLS-1$
            prefix    = XMLHandler.getTagValue(stepnode, "prefix"); //$NON-NLS-1$

            addingLineNrInGroup = "Y".equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "add_linenr")); // $NON-NLS-1$
            lineNrInGroupField = XMLHandler.getTagValue(stepnode, "linenr_fieldname");
            
			Node groupn = XMLHandler.getSubNode(stepnode, "group"); //$NON-NLS-1$
			Node fields = XMLHandler.getSubNode(stepnode, "fields"); //$NON-NLS-1$
			
			int sizegroup = XMLHandler.countNodes(groupn, "field"); //$NON-NLS-1$
			int nrfields  = XMLHandler.countNodes(fields, "field"); //$NON-NLS-1$
			
			allocate(sizegroup, nrfields);
	
			for (int i=0;i<sizegroup;i++)
			{
				Node fnode = XMLHandler.getSubNodeByNr(groupn, "field", i); //$NON-NLS-1$
				groupField[i]  = XMLHandler.getTagValue(fnode, "name");		 //$NON-NLS-1$
			}
	
			for (int i=0;i<nrfields;i++)
			{
				Node fnode = XMLHandler.getSubNodeByNr(fields, "field", i); //$NON-NLS-1$
				aggregateField[i]  = XMLHandler.getTagValue(fnode, "aggregate");		 //$NON-NLS-1$
				subjectField[i]    = XMLHandler.getTagValue(fnode, "subject");		 //$NON-NLS-1$
				aggregateType[i]       = getType(XMLHandler.getTagValue(fnode, "type"));	 //$NON-NLS-1$
			}
		}
		catch(Exception e)
		{
			throw new KettleXMLException(Messages.getString("GroupByMeta.Exception.UnableToLoadStepInfoFromXML"), e); //$NON-NLS-1$
		}
	}
	
	public static final int getType(String desc)
	{
		for (int i=0;i<typeGroupCode.length;i++)
		{
			if (typeGroupCode[i].equalsIgnoreCase(desc)) return i;
		}
		for (int i=0;i<typeGroupLongDesc.length;i++)
		{
			if (typeGroupLongDesc[i].equalsIgnoreCase(desc)) return i;
		}
		return 0;
	}

	public static final String getTypeDesc(int i)
	{
		if (i<0 || i>=typeGroupCode.length) return null;
		return typeGroupCode[i];
	}

	public static final String getTypeDescLong(int i)
	{
		if (i<0 || i>=typeGroupLongDesc.length) return null;
		return typeGroupLongDesc[i];
	}
	
	public void setDefault()
	{
        directory="%%java.io.tmpdir%%"; //$NON-NLS-1$
        prefix="grp"; //$NON-NLS-1$
        
		passAllRows    = false;
		aggregateIgnored = false;
		aggregateIgnoredField     = null;

		int sizegroup= 0;
		int nrfields = 0;
		
		allocate( sizegroup, nrfields );
	}

	public void getFields(RowMetaInterface r, String name, RowMetaInterface[] info, StepMeta nextStep, VariableSpace space)
	{
		// Loop over the aggregate names.
		// Keep the fields to group over and the aggregates
		RowMetaInterface rowOrig = (RowMetaInterface)r.clone();
		
        if (!passAllRows)
        {
    		// Remove those that are not in group[] (or subject in case we want all rows...)
    		for (int i=r.size()-1;i>=0;i--)
    		{
                String fieldName = rowOrig.getFieldNames()[i]; //###JB
                
    			boolean found=false;
    			for (int j=0;j<groupField.length && !found;j++)
    			{
    				if (fieldName.equalsIgnoreCase(groupField[j])) found=true;
    			}
    			if (!found) r.removeValueMeta(i);
    		}
        }
		
		// Re-add aggregates
		for (int i=0;i<subjectField.length;i++)
		{
			ValueMetaInterface subj = rowOrig.searchValueMeta(subjectField[i]);
			if (subj!=null)
			{
				// System.out.println("found subject #"+i+" --> "+subj);
				String value_name = aggregateField[i];
				int value_type = ValueMetaInterface.TYPE_NONE;
                int length = -1;
                int precision = -1;
                
				switch(aggregateType[i])
				{
					case TYPE_GROUP_SUM             : value_type = subj.getType(); break;
					case TYPE_GROUP_AVERAGE         :
					case TYPE_GROUP_COUNT_ALL       : value_type = ValueMetaInterface.TYPE_NUMBER; break;
                    case TYPE_GROUP_FIRST           : 
                    case TYPE_GROUP_LAST            : 
                    case TYPE_GROUP_FIRST_INCL_NULL : 
                    case TYPE_GROUP_LAST_INCL_NULL  : 
					case TYPE_GROUP_MIN             : 
					case TYPE_GROUP_MAX             : value_type = subj.getType(); break;
                    case TYPE_GROUP_CONCAT_COMMA    : value_type = ValueMetaInterface.TYPE_STRING; break;
					default: break;
				}
                
                if (aggregateType[i]!=TYPE_GROUP_COUNT_ALL)
                {
                    length = subj.getLength();
                    precision = subj.getPrecision();
                }
                
				if (value_type != ValueMetaInterface.TYPE_NONE)
				{
					ValueMetaInterface v = new ValueMeta(value_name, value_type);
					v.setOrigin(name);
                    v.setLength(length, precision);
                    r.addValueMeta(v);
				}
			}
		}
        
        if (passAllRows)
        {
            // If we pass all rows, we can add a line nr in the group...
            if (addingLineNrInGroup && !Const.isEmpty(lineNrInGroupField))
            {
            	ValueMetaInterface lineNr = new ValueMeta(lineNrInGroupField, ValueMetaInterface.TYPE_INTEGER);
                lineNr.setLength(9);
                lineNr.setOrigin(name);
                r.addValueMeta(lineNr);
            }
        }

		return;
	}

	public String getXML()
	{
        StringBuffer retval = new StringBuffer(200);

		retval.append("      ").append(XMLHandler.addTagValue("all_rows",  passAllRows)); //$NON-NLS-1$ //$NON-NLS-2$
		retval.append("      ").append(XMLHandler.addTagValue("ignore_aggregate",  aggregateIgnored)); //$NON-NLS-1$ //$NON-NLS-2$
		retval.append("      ").append(XMLHandler.addTagValue("field_ignore", aggregateIgnoredField)); //$NON-NLS-1$ //$NON-NLS-2$
        retval.append("      ").append(XMLHandler.addTagValue("directory", directory)); //$NON-NLS-1$ //$NON-NLS-2$
        retval.append("      ").append(XMLHandler.addTagValue("prefix",    prefix)); //$NON-NLS-1$ //$NON-NLS-2$
        retval.append("      ").append(XMLHandler.addTagValue("add_linenr",  addingLineNrInGroup)); //$NON-NLS-1$ //$NON-NLS-2$
        retval.append("      ").append(XMLHandler.addTagValue("linenr_fieldname", lineNrInGroupField)); //$NON-NLS-1$
        
		retval.append("      <group>").append(Const.CR); //$NON-NLS-1$
		for (int i=0;i<groupField.length;i++)
		{
			retval.append("        <field>").append(Const.CR); //$NON-NLS-1$
			retval.append("          ").append(XMLHandler.addTagValue("name", groupField[i])); //$NON-NLS-1$ //$NON-NLS-2$
			retval.append("        </field>").append(Const.CR); //$NON-NLS-1$
		}
		retval.append("        </group>").append(Const.CR); //$NON-NLS-1$

		retval.append("      <fields>").append(Const.CR); //$NON-NLS-1$
		for (int i=0;i<subjectField.length;i++)
		{
			retval.append("        <field>").append(Const.CR); //$NON-NLS-1$
			retval.append("          ").append(XMLHandler.addTagValue("aggregate", aggregateField[i])); //$NON-NLS-1$ //$NON-NLS-2$
			retval.append("          ").append(XMLHandler.addTagValue("subject", subjectField[i])); //$NON-NLS-1$ //$NON-NLS-2$
			retval.append("          ").append(XMLHandler.addTagValue("type", getTypeDesc(aggregateType[i]))); //$NON-NLS-1$ //$NON-NLS-2$
			retval.append("        </field>").append(Const.CR); //$NON-NLS-1$
		}
		retval.append("        </fields>").append(Const.CR); //$NON-NLS-1$

		return retval.toString();
	}
	
	public void readRep(Repository rep, long id_step, List<DatabaseMeta> databases, Map<String, Counter> counters)
		throws KettleException
	{
		try
		{
			passAllRows         = rep.getStepAttributeBoolean(id_step, "all_rows"); //$NON-NLS-1$
			aggregateIgnored = rep.getStepAttributeBoolean(id_step, "ignore_aggregate"); //$NON-NLS-1$
			aggregateIgnoredField     = rep.getStepAttributeString (id_step, "field_ignore"); //$NON-NLS-1$
            directory        =      rep.getStepAttributeString (id_step, "directory"); //$NON-NLS-1$
            prefix           =      rep.getStepAttributeString (id_step, "prefix"); //$NON-NLS-1$
            addingLineNrInGroup = rep.getStepAttributeBoolean(id_step, "add_linenr"); // $NON-NLS-1$
            lineNrInGroupField = rep.getStepAttributeString(id_step, "linenr_fieldname"); // $NON-NLS-1$
            
			int groupsize = rep.countNrStepAttributes(id_step, "group_name"); //$NON-NLS-1$
			int nrvalues  = rep.countNrStepAttributes(id_step, "aggregate_name"); //$NON-NLS-1$
			
			allocate(groupsize, nrvalues);
			
			for (int i=0;i<groupsize;i++)
			{
				groupField[i] = rep.getStepAttributeString(id_step, i, "group_name"); //$NON-NLS-1$
			}
	
			for (int i=0;i<nrvalues;i++)
			{
				aggregateField[i] = rep.getStepAttributeString(id_step, i, "aggregate_name"); //$NON-NLS-1$
				subjectField[i]   = rep.getStepAttributeString(id_step, i, "aggregate_subject"); //$NON-NLS-1$
				aggregateType[i]      = getType( rep.getStepAttributeString(id_step, i, "aggregate_type") ); //$NON-NLS-1$
			}
		}
		catch(Exception e)
		{
			throw new KettleException(Messages.getString("GroupByMeta.Exception.UnexpectedErrorInReadingStepInfoFromRepository"), e); //$NON-NLS-1$
		}
	}

	public void saveRep(Repository rep, long id_transformation, long id_step)
		throws KettleException
	{
		try
		{
			rep.saveStepAttribute(id_transformation, id_step, "all_rows",         passAllRows); //$NON-NLS-1$
			rep.saveStepAttribute(id_transformation, id_step, "ignore_aggregate", aggregateIgnored); //$NON-NLS-1$
			rep.saveStepAttribute(id_transformation, id_step, "field_ignore",     aggregateIgnoredField); //$NON-NLS-1$
            rep.saveStepAttribute(id_transformation, id_step, "directory",        directory); //$NON-NLS-1$
            rep.saveStepAttribute(id_transformation, id_step, "prefix",           prefix); //$NON-NLS-1$
            rep.saveStepAttribute(id_transformation, id_step, "add_linenr",       addingLineNrInGroup); // $NON-NLS-1$
            rep.saveStepAttribute(id_transformation, id_step, "linenr_fieldname", lineNrInGroupField); // $NON-NLS-1$
            
			for (int i=0;i<groupField.length;i++)
			{
				rep.saveStepAttribute(id_transformation, id_step, i, "group_name",       groupField[i]); //$NON-NLS-1$
			}
	
			for (int i=0;i<subjectField.length;i++)
			{
				rep.saveStepAttribute(id_transformation, id_step, i, "aggregate_name",    aggregateField[i]); //$NON-NLS-1$
				rep.saveStepAttribute(id_transformation, id_step, i, "aggregate_subject", subjectField[i]); //$NON-NLS-1$
				rep.saveStepAttribute(id_transformation, id_step, i, "aggregate_type",    getTypeDesc(aggregateType[i])); //$NON-NLS-1$
			}
		}
		catch(Exception e)
		{
			throw new KettleException(Messages.getString("GroupByMeta.Exception.UnableToSaveStepInfoToRepository")+id_step, e); //$NON-NLS-1$
		}
	}

	
	public void check(List<CheckResult> remarks, TransMeta transMeta, StepMeta stepMeta, RowMetaInterface prev, String input[], String output[], RowMetaInterface info)
	{
		CheckResult cr;

		if (input.length>0)
		{
			cr = new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString("GroupByMeta.CheckResult.ReceivingInfoOK"), stepMeta); //$NON-NLS-1$
			remarks.add(cr);
		}
		else
		{
			cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, Messages.getString("GroupByMeta.CheckResult.NoInputError"), stepMeta); //$NON-NLS-1$
			remarks.add(cr);
		}
	}
	
	public StepDialogInterface getDialog(Shell shell, StepMetaInterface info, TransMeta transMeta, String name)
	{
		return new GroupByDialog(shell, info, transMeta, name);
	}

	public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta transMeta, Trans trans)
	{
		return new GroupBy(stepMeta, stepDataInterface, cnr, transMeta, trans);
	}

	public StepDataInterface getStepData()
	{
		return new GroupByData();
	}

    /**
     * @return Returns the directory.
     */
    public String getDirectory()
    {
        return directory;
    }

    /**
     * @param directory The directory to set.
     */
    public void setDirectory(String directory)
    {
        this.directory = directory;
    }

    /**
     * @return Returns the prefix.
     */
    public String getPrefix()
    {
        return prefix;
    }

    /**
     * @param prefix The prefix to set.
     */
    public void setPrefix(String prefix)
    {
        this.prefix = prefix;
    }

    /**
     * @return the addingLineNrInGroup
     */
    public boolean isAddingLineNrInGroup()
    {
        return addingLineNrInGroup;
    }

    /**
     * @param addingLineNrInGroup the addingLineNrInGroup to set
     */
    public void setAddingLineNrInGroup(boolean addingLineNrInGroup)
    {
        this.addingLineNrInGroup = addingLineNrInGroup;
    }

    /**
     * @return the lineNrInGroupField
     */
    public String getLineNrInGroupField()
    {
        return lineNrInGroupField;
    }

    /**
     * @param lineNrInGroupField the lineNrInGroupField to set
     */
    public void setLineNrInGroupField(String lineNrInGroupField)
    {
        this.lineNrInGroupField = lineNrInGroupField;
    }
}