package bio.ferlab.clin.qlinme.cients;

import bio.ferlab.clin.qlinme.App;
import bio.ferlab.clin.qlinme.model.Metadata;
import bio.ferlab.clin.qlinme.utils.DateUtils;
import bio.ferlab.clin.qlinme.utils.S3TimedCache;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.PerformanceOptionsEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.hl7.fhir.r4.model.*;

import java.util.*;

@Slf4j
public class FhirClient {

  private static final int FETCH_SIZE = 100;
  // https://www.notion.so/ferlab/e6a3033cd3634b0d8948e1e4fde833e5?v=1afe0ff37cc943aa8c1144e2bb1bc649
  private static final List<String> PANELS_TO_IGNORE = List.of("EXTUM", "RGDI+", "SCID", "SHEMA", "SSOLID", "TRATU", "");

  private final FhirContext context;
  private final IGenericClient genericClient;
  private final S3TimedCache cache;

  public FhirClient(String url, int timeoutMs, int poolSize, S3TimedCache cache) {
    this.cache = cache;
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
    return cache.get("fhir.panels", new TypeReference<List<String>>() { }).filter(c -> allowCache).orElseGet(() -> {
      var response = this.genericClient.read().resource(CodeSystem.class).withId("analysis-request-code").withAdditionalHeader(HttpHeaders.AUTHORIZATION, rpt).execute();
      var values = response.getConcept().stream().map(CodeSystem.ConceptDefinitionComponent::getCode)
        .filter(c -> !PANELS_TO_IGNORE.contains(c)).sorted().toList();
      log.info("Fetched panels: {}", values);
      return cache.put("fhir.panels", values);
    });
  }

  public synchronized List<String> getOrganizations(String rpt, boolean allowCache) {
    return cache.get("fhir.organizations",  new TypeReference<List<String>>() { }).filter(c -> allowCache).orElseGet(() -> {
      var response = this.genericClient.search().forResource(Organization.class).count(FETCH_SIZE).returnBundle(Bundle.class).withAdditionalHeader(HttpHeaders.AUTHORIZATION, rpt).execute();
      var values = response.getEntry().stream().map(e -> (Organization)e.getResource()).map(o -> o.getIdElement().getIdPart()).sorted().toList();
      log.info("Fetched organizations: {}", values);
      return cache.put("fhir.organizations", values);
    });
  }

  public synchronized Map<String, List<String>> getAliquotIDsByBatch(String rpt, boolean allowCache) {
    return cache.get("fhir.aliquotids",  new TypeReference<Map<String, List<String>>>() { }).filter(c -> allowCache).orElseGet(() -> {
      var values = new TreeMap<String, List<String>>();
      fetchAliquotIDs(rpt, values, FETCH_SIZE, 0);
      log.info("Fetched aliquotids: {}", values.values().stream().flatMap(List::stream).toList().size());
      return cache.put("fhir.aliquotids", values);
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

  public synchronized List<Metadata.Patient> getPatients(String rpt, boolean allowCache) {
    return cache.get("fhir.patients",   new TypeReference<List<Metadata.Patient>>(){}).filter(c -> allowCache).orElseGet(() -> {
      var values = new ArrayList<Metadata.Patient>();
      fetchPatients(rpt, values, FETCH_SIZE / 2, 0);
      log.info("Fetched patients: {}", values.size());
      return cache.put("fhir.patients", values);
    });
  }

  private void fetchPatients(String rpt, List<Metadata.Patient> allPatients, int size, int offset) {
    var response = this.genericClient.search().forResource(Patient.class).revInclude(Person.INCLUDE_PATIENT).count(size).offset(offset).returnBundle(Bundle.class).withAdditionalHeader(HttpHeaders.AUTHORIZATION, rpt).execute();
    var patients = response.getEntry().stream().filter(e -> e.getResource() instanceof Patient).map(e -> (Patient)e.getResource()).toList();
    var persons = response.getEntry().stream().filter(e -> e.getResource() instanceof Person).map(e -> (Person)e.getResource()).toList();
    if (!patients.isEmpty()) {
      for(var patient: patients) {
        var person = persons.stream().filter(p -> p.getLink().stream().anyMatch(l -> l.getTarget().getReference().equals("Patient/"+patient.getIdElement().getIdPart()))).findFirst().orElse(new Person());
        allPatients.add(new Metadata.Patient(
          Optional.ofNullable(person.getNameFirstRep()).map(HumanName::getGivenAsSingleString).orElse(null),
          Optional.ofNullable(person.getNameFirstRep()).map(HumanName::getFamily).orElse(null),
          Optional.ofNullable(person.getGender()).map(Enumerations.AdministrativeGender::toCode).orElse(null),
          person.getIdentifier().stream().filter(i -> "JHN".equals(i.getType().getCodingFirstRep().getCode())).findFirst().map(Identifier::getValue).orElse(null),
          Optional.ofNullable(person.getBirthDate()).map(d -> DateUtils.format(d, DateUtils.DDMMYYYY)).orElse(null),
          patient.getIdentifier().stream().filter(i -> "MR".equals(i.getType().getCodingFirstRep().getCode())).findFirst().map(Identifier::getValue).orElse(null),
          Optional.ofNullable(patient.getManagingOrganization()).map(o -> o.getReference().replace("Organization/", "")).orElse(null),
          null,
          null,
          null,
          false
          ));
      }
      fetchPatients(rpt, allPatients, size, offset + size);
    }
  }

}
