package cz.fi.muni.jboss.Migration.ConnectionFactories;

import javax.xml.bind.annotation.*;
import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: Roman Jakubco
 * Date: 8/28/12
 * Time: 3:27 PM
 */
@XmlRootElement(name = "connection-factories")
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "connection-factories")
public class ConnectionFactories {
    @XmlElement(name = "tx-connection-factory")
    private Collection<ConnectionFactoryAS5> connectionFactories;


    //No idea for now what is this. But it can be a element in AS5
    //TODO: find more about this element. Can it be transform to As7?
    @XmlElement(name = "no-tx-connection-factory")
    private String pom;

    public Collection<ConnectionFactoryAS5> getConnectionFactories() {
        return connectionFactories;
    }

    public void setConnectionFactories(Collection<ConnectionFactoryAS5> connectionFactories) {
        this.connectionFactories = connectionFactories;
    }
}
