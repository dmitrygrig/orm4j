/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package orm4j;

import java.util.ArrayList;
import java.util.List;
import orm4j.query.INamedQueryManager;
import orm4j.query.NamedQueryManager;
import orm4j.util.GlobalController;

/**
 *
 * @author grig
 */
public class Configuration {
    private IDataProvider dataProvider;
    private INamedQueryManager queryManager;
    private List<Class> mappings;

    private Configuration() {
    }
    
    public Configuration(IDataProvider dataProvider, INamedQueryManager queryManager, List<Class> mappings) {
        this.dataProvider = dataProvider;
        this.queryManager = queryManager;
        this.mappings = mappings;
    }
    
    public static Configuration getDefault(){
        Configuration conf = new Configuration();
        INamedQueryManager manager = new NamedQueryManager();
        conf.setQueryManager(manager);
        conf.setDataProvider(new SqliteDataProvider(manager));
        conf.setMappings(new ArrayList<Class>());
        
        return conf;
    }
    
    public IDataContext createDataContext(){
        for (Class c : mappings) {
            queryManager.registerClass(c);
        }
        
        return new DataContext(dataProvider, queryManager);
    }

    public IDataProvider getDataProvider() {
        return dataProvider;
    }

    public void setDataProvider(IDataProvider dataProvider) {
        this.dataProvider = dataProvider;
    }

    public INamedQueryManager getQueryManager() {
        return queryManager;
    }

    public void setQueryManager(INamedQueryManager queryManager) {
        this.queryManager = queryManager;
    }

    public List<Class> getMappings() {
        return mappings;
    }

    public void setMappings(List<Class> mappings) {
        this.mappings = mappings;
    }
    
    public void addMappingFromPackage(String packageName){
        this.getMappings().addAll(GlobalController.getClassesForPackage(packageName));
    }
    
    public void setDatabasePath(String databasePath){
        this.dataProvider.setDatabaseURL(databasePath);
    }
}
