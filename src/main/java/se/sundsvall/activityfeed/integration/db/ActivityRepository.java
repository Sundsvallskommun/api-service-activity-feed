package se.sundsvall.activityfeed.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.activityfeed.integration.db.model.ActivityEntity;

@CircuitBreaker(name = "activityRepository")
@Transactional(readOnly = true)
public interface ActivityRepository extends JpaRepository<ActivityEntity, String>, JpaSpecificationExecutor<ActivityEntity> {

	Optional<ActivityEntity> findByMunicipalityIdAndActivityId(String municipalityId, String activityId);

	long countByMunicipalityId(String municipalityId);
}
