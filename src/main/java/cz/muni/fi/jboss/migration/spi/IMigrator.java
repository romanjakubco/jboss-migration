package cz.muni.fi.jboss.migration.spi;

import cz.muni.fi.jboss.migration.conf.Configuration;
import cz.muni.fi.jboss.migration.conf.GlobalConfiguration;
import cz.muni.fi.jboss.migration.MigrationContext;
import cz.muni.fi.jboss.migration.ex.ApplyMigrationException;
import cz.muni.fi.jboss.migration.ex.CliScriptException;
import cz.muni.fi.jboss.migration.ex.LoadMigrationException;
import cz.muni.fi.jboss.migration.ex.NodeGenerationException;
import org.w3c.dom.Node;

import java.util.List;

/**
 * A Migrator is responsible for 
 *   <ul>
 *   <li> reading the necessary data from AS, according to given configuration
 *   <li> transforming these data into it's own metamodel objects
 *   <li> providing the corresponding representation of data (CLI commands) for the target server (AS 7)
 *   <li> process the provided given custom config property
 *   </ul>
 *
 * @author Roman Jakubco
 */

public interface IMigrator {
    
    public GlobalConfiguration getGlobalConfig();
    public void setGlobalConfig( GlobalConfiguration conf );

    
    /**
     * Method for loading all files from AS5 and converting them to objects for migration which are then stored in Mig-
     * rationContext
     *
     * @param ctx context of migration with necessary object and information
     * @throws LoadMigrationException if loading of AS5 configuration fails (missing files / cannot read / wrong content)
     */
    public void loadAS5Data(MigrationContext ctx) throws LoadMigrationException;

    
    /**
     * Method for inserting migrated data to fresh standalone file.
     *
     * @param ctx context of migration with necessary object and information
     * @throws ApplyMigrationException if inserting of generated nodes fails
     */
    public void apply(MigrationContext ctx) throws ApplyMigrationException;

    
    /**
     * Method for generating Dom nodes from data stored in MigrationContext. Basically method representing actual migration
     * of the XML configuration files from AS5 to AS7.
     *
     * @param ctx context of migration with necessary object and information
     * @return List of all nodes, which represent migrated configuration of AS5
     * @throws NodeGenerationException if something when wrong with the migration of data and generation of Dom Nodes
     */
    public List<Node> generateDomElements(MigrationContext ctx) throws NodeGenerationException;

    
    /**
     * Generates CLI scripts from migrated data, which are generated by generateDomElements.
     *
     * @param ctx context of migration with necessary object and information
     * @return List of CLI scripts, which represent migrated configuration of AS5
     * @throws CliScriptException if required attributes for creation of scripts are missing
     */
    public List<String> generateCliScripts(MigrationContext ctx) throws CliScriptException;

    
    /**
     *  Examines a configuration property, typically acquired as console app params.
     * 
     *  @param moduleOption  It's value May be null, e.g. if the property didn't have '=value' part.
     *  @returns  0 if the property wasn't recognized, non-zero otherwise.
     */
    public int examineConfigProperty(Configuration.ModuleSpecificProperty moduleOption);
    
    
}// class
