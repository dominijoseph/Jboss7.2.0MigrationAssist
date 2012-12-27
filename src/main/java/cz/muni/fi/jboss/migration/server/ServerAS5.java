package cz.muni.fi.jboss.migration.server;

import org.eclipse.persistence.oxm.annotations.XmlPath;

import javax.xml.bind.annotation.*;
import java.util.Collection;

/**
 * 
 * @author  Roman Jakubco
 * Date: 8/30/12
 * Time: 4:54 PM
 */


@XmlRootElement(name = "Server")
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "Server")
public class ServerAS5 {
    @XmlElements(@XmlElement(name = "Service", type =Service.class))
    private Collection<Service> services;

    public Collection<Service> getServices() {
        return services;
    }

    public void setServices(Collection<Service> services) {
        this.services = services;
    }

    @Override
    public String toString() {
        return "ServerAS5{" +
                "services=" + services +
                '}';
    }






}