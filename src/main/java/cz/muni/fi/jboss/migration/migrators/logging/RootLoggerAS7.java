package cz.muni.fi.jboss.migration.migrators.logging;

import cz.muni.fi.jboss.migration.spi.IMigratedData;
import org.eclipse.persistence.oxm.annotations.XmlPath;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Set;

/**
 * @author Roman Jakubco
 *         Date: 1/24/13
 *         Time: 5:22 PM
 */

@XmlRootElement(name = "root-logger" )
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "root-logger")

public class RootLoggerAS7 implements IMigratedData {

    @XmlPath("level/@name")
    private String rootLoggerLevel;

    @XmlPath("handlers/handler/@name")
    private Set<String> rootLoggerHandlers;

    @XmlPath("filter/@value")
    private String rootLogFilValue;

    public String getRootLoggerLevel() {
        return rootLoggerLevel;
    }

    public void setRootLoggerLevel(String rootLoggerLevel) {
        this.rootLoggerLevel = rootLoggerLevel;
    }

    public Set<String> getRootLoggerHandlers() {
        return rootLoggerHandlers;
    }

    public void setRootLoggerHandlers(Set<String> rootLoggerHandlers) {
        this.rootLoggerHandlers = rootLoggerHandlers;
    }

    public String getRootLogFilValue() {
        return rootLogFilValue;
    }

    public void setRootLogFilValue(String rootLogFilValue) {
        this.rootLogFilValue = rootLogFilValue;
    }
}
