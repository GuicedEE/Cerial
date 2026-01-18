module guiced.cerial.test {
    requires com.guicedee.cerial;
    requires com.guicedee.guicedinjection;
    requires com.guicedee.services.health;
    requires io.opentelemetry.api;

    requires org.junit.platform.commons;
    requires org.junit.jupiter.api;
    requires org.mockito;
    requires org.mockito.junit.jupiter;
    requires com.fazecast.jSerialComm;

    opens com.guicedee.cerial.test to org.junit.platform.commons;
}