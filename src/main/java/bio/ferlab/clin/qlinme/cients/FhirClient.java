package bio.ferlab.clin.qlinme.cients;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.PerformanceOptionsEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;

public class FhirClient {

  private final FhirContext context;
  private final IGenericClient genericClient;

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
}
