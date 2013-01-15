package cz.muni.fi.jboss.Migration.DataSources;

import javax.xml.bind.annotation.*;

/**
 * @author: Roman Jakubco
 * Date: 10/2/12
 * Time: 8:59 PM
 */

@XmlRootElement(name = "xa-datasource-property")
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "xa-datasource-property")

public class  XaDatasourceProperty {

    @XmlValue
    private String xaDatasourceProp;
    @XmlAttribute(name = "name")
    private String xaDatasourcePropName;

    public String getXaDatasourceProp() {
        return xaDatasourceProp;
    }

    public void setXaDatasourceProp(String xaDatasourceProp) {
        this.xaDatasourceProp = xaDatasourceProp;
    }

    public String getXaDatasourcePropName() {
        return xaDatasourcePropName;
    }

    public void setXaDatasourcePropName(String xaDatasourcePropName) {
        this.xaDatasourcePropName = xaDatasourcePropName;
    }

}
