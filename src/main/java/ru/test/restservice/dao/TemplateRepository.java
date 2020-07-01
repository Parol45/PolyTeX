package ru.test.restservice.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.test.restservice.entity.Template;

import java.util.UUID;

public interface TemplateRepository extends JpaRepository<Template, UUID> {
}
