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
import org.hl7.fhir.r4.model.Task;

import java.util.*;

@Slf4j
public class FhirClient {

  private final FhirContext context;
  private final IGenericClient genericClient;
  private final TimedCache<String, Object> cache = new TimedCache<>(3600);

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
    return (List<String>) cache.get("panels").filter(c -> allowCache).orElseGet(() -> {
      var response = this.genericClient.read().resource(CodeSystem.class).withId("analysis-request-code").withAdditionalHeader(HttpHeaders.AUTHORIZATION, rpt).execute();
      var values = response.getConcept().stream().map(CodeSystem.ConceptDefinitionComponent::getCode).sorted().toList();
      log.info("Fetched panels: {}", values);
      return cache.put("panels", values);
    });
  }

  public synchronized List<String> getOrganizations(String rpt, boolean allowCache) {
    return (List<String>) cache.get("organizations").filter(c -> allowCache).orElseGet(() -> {
      var response = this.genericClient.search().forResource(Organization.class).count(100).returnBundle(Bundle.class).withAdditionalHeader(HttpHeaders.AUTHORIZATION, rpt).execute();
      var values = response.getEntry().stream().map(e -> (Organization)e.getResource()).map(o -> o.getIdElement().getIdPart()).sorted().toList();
      log.info("Fetched organizations: {}", values);
      return cache.put("organizations", values);
    });
  }

  public synchronized Map<String, List<String>> getAliquotIDsByBatch(String rpt, boolean allowCache) {
    return (Map<String, List<String>>) cache.get("aliquotids").filter(c -> allowCache).orElseGet(() -> {
      var values = new TreeMap<String, List<String>>();
      fetchAliquotIDs(rpt, values, 100, 0);
      log.info("Fetched aliquotids: {}", values.values().stream().flatMap(List::stream).toList().size());
      return cache.put("aliquotids", values);
    });
  }

  private void fetchAliquotIDs(String rpt, Map<String, List<String>> aliquotIDsByBatchID, int size, int offset) {
    var response = this.genericClient.search().forResource(Task.class).count(size).offset(offset).returnBundle(Bundle.class).withAdditionalHeader(HttpHeaders.AUTHORIZATION, rpt).execute();
    var values = response.getEntry().stream().map(e -> (Task)e.getResource()).toList();
    if (!values.isEmpty()) {
      values
        .forEach(t -> {
          var batchId  = t.getGroupIdentifier().getValue();
          var aliquotID = t.getExtensionByUrl("http://fhir.cqgc.ferlab.bio/StructureDefinition/sequencing-experiment").getExtensionByUrl("labAliquotId").getValue().toString();
          aliquotIDsByBatchID.computeIfAbsent(batchId, k -> new ArrayList<>());
          aliquotIDsByBatchID.get(batchId).add(aliquotID);
      });
      fetchAliquotIDs(rpt, aliquotIDsByBatchID, size, offset + size);
    }
  }

}
