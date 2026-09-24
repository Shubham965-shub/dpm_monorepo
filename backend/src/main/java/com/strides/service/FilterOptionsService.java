package com.strides.service;

import com.strides.repository.SiteOptionsRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class FilterOptionsService {

    @Inject
    SiteOptionsRepository siteOptionsRepository;

    public List<String> getSites() {
        return siteOptionsRepository.findAll();
    }
}
