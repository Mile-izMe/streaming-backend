package com.melody.melody_stream.entity.primary;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;

public class HibernateFilterConfig {

    @PersistenceContext
    private EntityManager entityManager;

    @PostConstruct
    public void enableSoftDeleteFilter() {
        Session session = entityManager.unwrap(Session.class);
        session.enableFilter("deletedFilter")
                .setParameter("isDeleted", false);
    }
}
