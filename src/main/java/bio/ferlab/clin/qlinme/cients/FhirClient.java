package bio.ferlab.clin.qlinme.cients;

import bio.ferlab.clin.qlinme.model.Metadata;
import bio.ferlab.clin.qlinme.utils.DateUtils;
import bio.ferlab.clin.qlinme.utils.S3TimedCache;
import bio.ferlab.clin.qlinme.utils.Utils;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.PerformanceOptionsEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.hl7.fhir.r4.model.*;

import java.util.*;

@Slf4j
public class FhirClient {

  private static final int FETCH_SIZE = 100;
  private static final int CHUNKED_SIZE = 10;
  // https://www.notion.so/ferlab/e6a3033cd3634b0d8948e1e4fde833e5?v=1afe0ff37cc943aa8c1144e2bb1bc649
  private static final List<String> PANELS_TO_IGNORE = List.of("RGDI+", "SCID", "SHEMA", "SSOLID", "TRATU");

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

  public synchronized Map<String, List<String>> getAliquotIDsByBatch(String rpt, List<String> aliquotIDs, boolean allowCache) {
    var chunkedIDs = Lists.partition(aliquotIDs, CHUNKED_SIZE);
    var aliquotIDsByBatchID = new TreeMap<String, List<String>>();
    chunkedIDs.forEach(ids -> {
      var values = fetchTaskByAliquotIDs(rpt, ids, allowCache);
      values.keySet().forEach(batchId -> {
        aliquotIDsByBatchID.computeIfAbsent(batchId, k -> new ArrayList<>());
        aliquotIDsByBatchID.get(batchId).addAll(values.get(batchId));
      });
    });
    log.info("Found aliquot IDs: {}", aliquotIDsByBatchID.values().stream().flatMap(List::stream).toList().size());
    return aliquotIDsByBatchID;
  }

  public synchronized Map<String, List<String>> getLdmServiceRequestId(String rpt, List<String> ldmServiceRequestIds, boolean allowCache) {
    var chunkedIDs = Lists.partition(ldmServiceRequestIds, CHUNKED_SIZE);
    var byBatchID = new TreeMap<String, List<String>>();
    chunkedIDs.forEach(ids -> {
      var values = fetchServiceRequestByIdentifiers(rpt, ids, allowCache);
      values.keySet().forEach(batchId -> {
        byBatchID.computeIfAbsent(batchId, k -> new ArrayList<>());
        byBatchID.get(batchId).addAll(values.get(batchId));
      });
    });
    log.info("Found ServiceRequest IDs: {}", byBatchID.values().stream().flatMap(List::stream).toList().size());
    return byBatchID;
  }

  private Map<String, List<String>> fetchTaskByAliquotIDs(String rpt, List<String> aliquotIDs, boolean allowCache) {
    if (aliquotIDs.isEmpty()) return new TreeMap<>();  // don't request fhir with empty query param
    var cacheKey = "fhir.aliquotids."+String.join("_", aliquotIDs);
    return cache.get(cacheKey,  new TypeReference<Map<String, List<String>>>() { }).filter(c -> allowCache).orElseGet(() -> {
      var response = this.genericClient.search().byUrl("Task?aliquotid=" + Utils.encodeURL(String.join(",", aliquotIDs))).count(aliquotIDs.size()).returnBundle(Bundle.class).withAdditionalHeader(HttpHeaders.AUTHORIZATION, rpt).execute();
      var aliquotIDsByBatchID = new TreeMap<String, List<String>>();
      response.getEntry().stream().map(e -> (Task)e.getResource())
        .forEach(t -> {
          var batchId  = t.getGroupIdentifier().getValue();
          var aliquotID = t.getExtensionByUrl("http://fhir.cqgc.ferlab.bio/StructureDefinition/sequencing-experiment").getExtensionByUrl("labAliquotId").getValue().toString();
          aliquotIDsByBatchID.computeIfAbsent(batchId, k -> new ArrayList<>());
          aliquotIDsByBatchID.get(batchId).add(aliquotID);
        });
      log.debug("Fetch aliquot IDs: {}", aliquotIDsByBatchID.values().stream().flatMap(List::stream).toList().size());
      return cache.put(cacheKey, aliquotIDsByBatchID);
    });
  }

  private Map<String, List<String>> fetchServiceRequestByIdentifiers(String rpt, List<String> ldmServiceRequestIds, boolean allowCache) {
    if (ldmServiceRequestIds.isEmpty()) return new TreeMap<>();  // don't request fhir with empty query param
    var cacheKey = "fhir.ldmServiceRequestIds."+String.join("_", ldmServiceRequestIds);
    return cache.get(cacheKey,  new TypeReference<Map<String, List<String>>>() { }).filter(c -> allowCache).orElseGet(() -> {
      var response = this.genericClient.search().byUrl("ServiceRequest?identifier=" + Utils.encodeURL(String.join(",", ldmServiceRequestIds)))
        .count(ldmServiceRequestIds.size()).revInclude(Task.INCLUDE_FOCUS).returnBundle(Bundle.class).withAdditionalHeader(HttpHeaders.AUTHORIZATION, rpt).execute();
      var byBatchID = new TreeMap<String, List<String>>();
      response.getEntry().forEach(e -> {
        if (e.getResource() instanceof Task t) {
          var focus = t.getFocus().getReference();
          var sr = response.getEntry().stream().filter(e2 -> e2.getResource() instanceof ServiceRequest s && ("ServiceRequest/"+s.getIdElement().getIdPart()).equals(focus)).map(e2 -> (ServiceRequest)e2.getResource()).findFirst().orElse(new ServiceRequest());
          var batchId  = t.getGroupIdentifier().getValue();
          byBatchID.computeIfAbsent(batchId, k -> new ArrayList<>());
          byBatchID.get(batchId).add(sr.getIdentifierFirstRep().getValue());
        }
      });
      log.debug("Fetch ServiceRequest IDs: {}", byBatchID.values().stream().flatMap(List::stream).toList().size());
      return cache.put(cacheKey, byBatchID);
    });
  }

  public synchronized List<Metadata.Patient> getPatients(String rpt, List<String> mrns, List<String> ramqs, boolean allowCache) {
    var values = new ArrayList<>(Lists.partition(mrns, CHUNKED_SIZE).stream().map(ids -> fetchPatientOrPersonByIdentifier(rpt, "Patient", ids, allowCache)).flatMap(Collection::stream).toList());
    var ignoredAlreadyFound = ramqs.stream().filter(ramq -> values.stream().noneMatch(alreadyFound -> ramq.equals(alreadyFound.ramq()))).toList();
    values.addAll(Lists.partition(ignoredAlreadyFound, CHUNKED_SIZE).stream().map(ids -> fetchPatientOrPersonByIdentifier(rpt, "Person", ids, allowCache)).flatMap(Collection::stream).toList());
    log.info("Found patients: {}", values.size());
    return values;
  }

  private List<Metadata.Patient> fetchPatientOrPersonByIdentifier(String rpt, String type, List<String> ids, boolean allowCache) {
    if (ids.isEmpty()) return List.of();  // don't request fhir with empty query param
    var cacheKey = "fhir."+type.toLowerCase()+"."+String.join("_", ids);
    return cache.get(cacheKey,  new TypeReference<List<Metadata.Patient>>() { }).filter(c -> allowCache).orElseGet(() -> {
      var response = this.genericClient.search().byUrl(type + "?identifier=" + Utils.encodeURL(String.join(",", ids))).revInclude(Person.INCLUDE_PATIENT).count(ids.size()).returnBundle(Bundle.class).withAdditionalHeader(HttpHeaders.AUTHORIZATION, rpt).execute();
      var patients = response.getEntry().stream().filter(e -> e.getResource() instanceof Patient).map(e -> (Patient) e.getResource()).toList();
      var persons = response.getEntry().stream().filter(e -> e.getResource() instanceof Person).map(e -> (Person) e.getResource()).toList();
      log.debug("Fetch patients: {}", patients.size());
      return cache.put(cacheKey, extractPatientInfo(patients, persons));
    });
  }

  private List<Metadata.Patient> extractPatientInfo(List<Patient> patients, List<Person> persons) {
    return patients.stream().map(patient -> {
      var person = persons.stream().filter(p -> p.getLink().stream().anyMatch(l -> l.getTarget().getReference().equals("Patient/" + patient.getIdElement().getIdPart()))).findFirst().orElse(new Person());
      return new Metadata.Patient(
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
      );
    }).toList();
  }

}
