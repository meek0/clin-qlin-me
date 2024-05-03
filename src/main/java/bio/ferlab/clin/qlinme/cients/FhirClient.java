package bio.ferlab.clin.qlinme.cients;

import bio.ferlab.clin.qlinme.utils.TimedCache;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.PerformanceOptionsEnum;
import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import io.javalin.http.Context;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Organization;

import java.util.Collections;
import java.util.List;

@Slf4j
public class FhirClient {

  private final FhirContext context;
  private final IGenericClient genericClient;
  private final TimedCache<String, List<String>> cache = new TimedCache<>(3600);

  public FhirClient(String url, int timeoutMs, int poolSize) {
    context = FhirContext.forR4();

    context.setPerformanceOptions(PerformanceOptionsEnum.DEFERRED_MODEL_SCANNING);
    context.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);

    context.getRestfulClientFactory().setConnectTimeout(timeoutMs);
    context.getRestfulClientFactory().setConnectionRequestTimeout(timeoutMs);
    context.getRestfulClientFactory().setSocketTimeout(timeoutMs);
    context.getRestfulClientFactory().setPoolMaxTotal(poolSize);
    context.getRestfulClientFactory().setPoolMaxPerRoute(poolSize);

    this.genericClient = context.newRestfulGenericClient(url);
  }

  public synchronized List<String> getPanelCodes(String rpt, boolean allowCache) {
    return cache.get("panels").filter(c -> allowCache).orElseGet(() -> {
      var response = this.genericClient.read().resource(CodeSystem.class).withId("analysis-request-code").withAdditionalHeader(HttpHeaders.AUTHORIZATION, rpt).execute();
      var values = response.getConcept().stream().map(CodeSystem.ConceptDefinitionComponent::getCode).sorted().toList();
      log.info("Fetched panels: {}", values);
      return cache.put("panels", values);
    });
  }

  public synchronized List<String> getOrganizations(String rpt, boolean allowCache) {
    return cache.get("organizations").filter(c -> allowCache).orElseGet(() -> {
      var response = this.genericClient.search().forResource(Organization.class).count(100).returnBundle(Bundle.class).withAdditionalHeader(HttpHeaders.AUTHORIZATION, rpt).execute();
      var values = response.getEntry().stream().map(e -> (Organization)e.getResource()).map(o -> o.getIdElement().getIdPart()).sorted().toList();
      log.info("Fetched Organizations: {}", values);
      return cache.put("organizations", values);
    });
  }

}
