package uk.ac.newcastle.enterprisemiddleware.booking;

import org.modelmapper.ModelMapper;
import javax.enterprise.inject.Produces;

public class ModelMapperProducer {

    @Produces
    public ModelMapper produceModelMapper() {
        return new ModelMapper();
    }
}
