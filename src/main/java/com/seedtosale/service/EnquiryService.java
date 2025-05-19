package com.seedtosale.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.seedtosale.model.Enquiry;
import com.seedtosale.repository.EnquiryRepository;

@Service
public class EnquiryService {

    @Autowired
    private EnquiryRepository enquiryRepository;

    public Enquiry saveEnquiry(Enquiry enquiry) {
        return enquiryRepository.save(enquiry);
    }

    public java.util.List<Enquiry> getAllEnquiry() {
        return enquiryRepository.findAll();
    }
}
