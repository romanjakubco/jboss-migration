package cz.muni.fi.jboss.migration.actions;

import cz.muni.fi.jboss.migration.ex.MigrationException;

import java.io.File;

/**
 *
 * @author Ondrej Zizka, ozizka at redhat.com
 */
public class ModuleCreationAction extends AbstractStatefulAction {
    
    File src;
    File dest;
    File moduleXml;
    boolean overwrite;


    public ModuleCreationAction( File src, File dest, File moduleXml, boolean overwrite ) {
        this.src = src;
        this.dest = dest;
        this.moduleXml = moduleXml;
        this.overwrite = overwrite;
    }
    
    
    @Override
    public void preValidate() throws MigrationException {
    }


    @Override
    public void perform() throws MigrationException {
        // Create a module.

        
         setState(State.DONE);

    }


    @Override
    public void rollback() throws MigrationException {
        //try {
            setState(State.ROLLED_BACK);
        //}
    }


    @Override
    public void postValidate() throws MigrationException {
        // 
    }


    @Override
    public void backup() throws MigrationException {
        setState( State.BACKED_UP );
    }


    @Override
    public void cleanBackup() {
        setState( State.FINISHED );
    }

}// class
