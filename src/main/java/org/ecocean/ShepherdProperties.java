package org.ecocean;

import org.ecocean.servlet.ServletUtilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Properties; 
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import org.ecocean.configuration.*;
import org.json.JSONObject;


public class ShepherdProperties {

  public static final String[] overrideOrgsArr = {"indocet"};
  // set for easy .contains() checking
  public static final Set<String> overrideOrgs = new HashSet<>(Arrays.asList(overrideOrgsArr));

  public static Properties getProperties(String fileName){
    return getProperties(fileName, "en");
  }
  
  public static Properties getProperties(String fileName, String langCode){
    return getProperties(fileName, langCode, "context0");
  }

  public static Properties getProperties(String fileName, String langCode, String context){
    return getProperties(fileName, langCode, context, null);
  }

  // This method works just like getProperties, except it gives priority to your organization-specific .properties overwrite file
  //   + It checks the logged-in user to see if they are in an organization with an overwrite .properties file
  //   + If they are, this overwrite .properties file gets priority over the default file.
  //   + overwrite files are in wildbook_data_dir/classes/bundles/<organization>.properties
  public static Properties getOrgProperties(String fileName, String langCode, String context, HttpServletRequest request) {
    return getProperties(fileName, langCode, context, getOverwriteStringForUser(request));
  }
  
  public static Properties getOrgProperties(String fileName, String langCode, String context, HttpServletRequest request, Shepherd myShepherd) {
    return getProperties(fileName, langCode, context, getOverwriteStringForUser(request, myShepherd));
  }
  
  public static Properties getOrgProperties(String fileName, String langCode, HttpServletRequest request) {
    return getOrgProperties(fileName, langCode, ServletUtilities.getContext(request), request);
  }

  // ONLY the overwrite props, not any other .properties
  public static Properties getOverwriteProps(HttpServletRequest request) {
    String filename = getOverwriteStringForUser(request);
    if (filename==null) return null;
    String fullPath = "webapps/wildbook_data_dir/WEB-INF/classes/bundles/"+filename;
    return (Properties)loadProperties(fullPath);
  }

  public static String getOverwriteStringForUser(HttpServletRequest request, Shepherd myShepherd) {
    if (request == null) return null;
    String manualOrgName = request.getParameter("organization");
    // manual request params
    if (Util.stringExists(manualOrgName)) {
      String overwrite = getOverwriteStringForOrgName(manualOrgName);
      if (Util.stringExists(overwrite)) return overwrite;
    }
    // now try based on the user's organizations
    User user = myShepherd.getUser(request);
    if (user==null) return null;
    return getOverwriteStringForUser(user);
  }
  
  public static String getOverwriteStringForUser(HttpServletRequest request) {
    Shepherd myShepherd = new Shepherd(request);
    myShepherd.setAction("getOverwriteStringForUser");
    myShepherd.beginDBTransaction();
    String myString=getOverwriteStringForUser(request,myShepherd);
    myShepherd.rollbackDBTransaction();
    myShepherd.closeDBTransaction();
    return myString;
  }
  
  
  
  
  public static String getOverwriteStringForUser(User user) {
    if (user == null || user.getOrganizations()==null) return null;
    for (Organization org: user.getOrganizations()) {
      String name = org.getName();
      if (name==null) continue;
      name = name.toLowerCase();
      if (overrideOrgs.contains(name)) return name+".properties";
    }
    return null;
  }

  public static String getOverwriteStringForOrgName(String orgName) {
    if (overrideOrgs.contains(orgName)) return orgName+".properties";
    return null;
  }

  public static boolean orgHasOverwrite(String orgName) {
    return (getOverwriteStringForOrgName(orgName) !=null);
  }


  public static boolean userHasOverrideString(User user) {
    return (getOverwriteStringForUser(user)!=null);
  }
  public static boolean userHasOverrideString(HttpServletRequest request) {
    return (getOverwriteStringForUser(request)!=null);
  }

    public static Properties getProperties(String fileName, String langCode, String context, String overridePrefix){
        System.out.println("ShepherdProperties.getProperties() is DEPRECATED; please consider upgrading code; called with: " + fileName + ", " + langCode + ", " + context + ", " + overridePrefix);
        if (fileName != null) throw new RuntimeException("bailing to prevent loop");
        if (Util.stringExists(langCode)) return getPropertiesLEGACY(fileName, langCode, context, overridePrefix);

        if ((fileName == null) || !fileName.endsWith(".properties")) throw new RuntimeException("ShepherdProperties.getProperties() invalid fileName=" + fileName);
        String id = fileName.substring(0, fileName.length() - 11);
        ShepherdRO myShepherd = new ShepherdRO(context);
        myShepherd.setAction("ShepherdProperties.getProperties");
        myShepherd.beginDBTransaction();
        Configuration conf = ConfigurationUtil.getConfiguration(myShepherd, id);
        myShepherd.rollbackDBTransaction();
        myShepherd.closeDBTransaction();
        if (conf == null) {  //throw new RuntimeException() <-- probably better
            System.out.println("ERROR: ShepherdProperties.getProperties() has no Configuration for id=" + id + " (fileName=" + fileName + ")");
            return null;
        }
        return __configurationContentToProperties(conf.getContent());
        //return getPropertiesLEGACY(fileName, langCode, context, overridePrefix);
    }

    private static Properties __configurationContentToProperties(JSONObject content) {
        if (content == null) return null;
        Properties prop = new Properties();
        for (Object k : content.keySet()) {
            String key = (String)k;
            JSONObject val = content.optJSONObject(key);
            if (val != null) prop.setProperty(key, val.optString("__value", null));
        }
System.out.println("INFO: __configurationContentToProperties() got Properties with size " + prop.stringPropertyNames().size());
        return prop;
    }

/*  LEGACY CRUFT for prosperity  */
  public static Properties getPropertiesLEGACY(String fileName, String langCode, String context, String overridePrefix){
        System.out.println("ShepherdProperties.getPropertiesLEGACY() is DEPRECATED; please consider upgrading code; called with: " + fileName + ", " + langCode + ", " + context + ", " + overridePrefix);
    // initialize props as empty (no override provided) or the default values (if override provided)
    // initializing
    boolean verbose = (Util.stringExists(overridePrefix));

    String shepherdDataDir="wildbook_data_dir";
    Properties contextsProps=getContextsProperties();
    if(contextsProps.getProperty(context+"DataDir")!=null) {
      shepherdDataDir=contextsProps.getProperty(context+"DataDir"); 
    }

    if (Util.stringExists(langCode) && !langCode.endsWith("/")) langCode += "/";

    // we load defaultProps from the (on git) webapps/wildbook location
    String defaultPathStr = "webapps/wildbook/WEB-INF/classes/bundles/"+langCode+fileName;
    // defaultProps are selectively overwritten from the override dir. This way we can keep
    // security-sensitive prop off github in the shepherdDataDir. Only need to store the sensitive fields there
    // bc we fall-back to the defaultProps
    String overridePathStr = "webapps/"+shepherdDataDir+"/WEB-INF/classes/bundles/"+langCode+fileName;

    //System.out.printf("getProperties has built strings %s and %s.\n",defaultPathStr, overridePathStr);

    Properties defaultProps = loadProperties(defaultPathStr);
    if (defaultProps==null) {
      // could not find props w this lang code, try english
      if (Util.stringExists(langCode) && !langCode.contains("en")) {
        defaultPathStr = "webapps/wildbook/WEB-INF/classes/bundles/en/"+fileName;
        System.out.printf("\t Weird Shepherd.properties non-english case reached with langCode %s. Trying again with path %s.\n",langCode,defaultPathStr);
        defaultProps = loadProperties(defaultPathStr);
      } else {
        System.out.printf("Super weird case met in ShepherdProperties.getProps(%s, %s, %s, %s). Returning generated default props.\n",fileName, langCode, context, overridePrefix);
      }
    }
    Properties props = loadProperties(overridePathStr, defaultProps);
    if (!Util.stringExists(overridePrefix)) return (Properties)props;

    // todo: now actually load the override string
    // we Do have an overridePrefix so we need to load it now
    String customUserPathString = "webapps/"+shepherdDataDir+"/WEB-INF/classes/bundles/"+overridePrefix;
    return (Properties)loadProperties(customUserPathString, props);
  }


  public static Properties loadProperties(String pathStr, Properties defaults) {
    //System.out.println("loadProperties called for path "+pathStr);
    File propertiesFile = new File(pathStr);
    if (propertiesFile == null || !propertiesFile.exists()) return defaults;
    try {
      InputStream inputStream = new FileInputStream(propertiesFile);
      if (inputStream == null) return null;
      LinkedProperties props = (defaults!=null) ? new LinkedProperties(defaults) : new LinkedProperties();
      props.load(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
      if (inputStream!=null) inputStream.close();
      return (Properties)props;
    } catch (Exception e) {
      System.out.println("Exception on loadProperties()");
      e.printStackTrace();
    }
    return defaults;
  }
  public static Properties loadProperties(String pathStr) {
    return loadProperties(pathStr, null);
  }

  public static Properties getContextsProperties(){
//System.out.println("DEPRECATED getContextsProperties() called");
    LinkedProperties props=new LinkedProperties();
      try {
        InputStream inputStream = ShepherdProperties.class.getResourceAsStream("/bundles/contexts.properties");
        props.load(inputStream);
        inputStream.close();
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }
    return (Properties)props;
  }


  public static List<String> getIndexedPropertyValues(Properties props, String baseKey) {
    List<String> list = new ArrayList<String>();
    boolean hasMore = true;
    int index = 0;
    while (hasMore) {
      String key = baseKey + index++;
      String value = props.getProperty(key);
      if (value != null) {
        if (value.length() > 0) {
          list.add(value.trim());
        }
      }
      else {
        hasMore = false;
      }
    }
    return list;
  }


}
