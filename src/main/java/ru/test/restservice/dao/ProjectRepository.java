package ru.test.restservice.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.test.restservice.entity.Project;

import java.util.UUID;

/**
 * Класс-репозиторий для общения с БД
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {
}
