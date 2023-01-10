// Código completo, ejecutado contra mi servidor jpa fhir server local o el de HAPI. 
// Para arrancar el servidor local, previamente abrir docker, y ejecutar desde línea de comandos:
// cd /Users/fnovillo/Projects/solutions/hapi-fhir-jpaserver-starter
// docker pull hapiproject/hapi:latest
// docker run -p 8080:8080 hapiproject/hapi:latest

package ca.uhn.fhir.letsbuild.upload;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import com.google.common.base.Charsets;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.SimpleQuantity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;

// This file contains an example of the complete day 1 and 2 solution
public class CsvDataUploader_HintComplete {

	private static final Logger ourLog = LoggerFactory.getLogger(CsvDataUploader_HintComplete.class);

	public static void main(String[] theArgs) throws Exception {

		// Create a FHIR client
		FhirContext ctx = FhirContext.forR4();
		// Servidor local (levantar con docker)
		// IGenericClient client =
		// ctx.newRestfulGenericClient("http://localhost:8080/fhir/");
		// Servidor HAPI
		IGenericClient client = ctx.newRestfulGenericClient("http://hapi.fhir.org/baseR4");
		client.registerInterceptor(new LoggingInterceptor(false));

		// Open the CSV file for reading
		try (InputStream inputStream = new FileInputStream("src/main/resources/sample-data.csv")) {
			Reader reader = new InputStreamReader(inputStream, Charsets.UTF_8);

			CSVFormat format = CSVFormat.EXCEL
					.withFirstRecordAsHeader()
					.withDelimiter(',');
			CSVParser csvParser = format.parse(reader);

			// Loop throw each row in the CSV file
			for (CSVRecord nextRecord : csvParser.getRecords()) {

				// Sequence number - This could be used as an ID for generated resources
				String seqN = nextRecord.get("SEQN");

				// Timestamp - This will be formatted in ISO8601 format
				String timestamp = nextRecord.get("TIMESTAMP");

				// Patient ID
				String patientId = nextRecord.get("PATIENT_ID");

				// Patient Family Name
				String patientFamilyName = nextRecord.get("PATIENT_FAMILYNAME");

				// Patient Given Name
				String patientGivenName = nextRecord.get("PATIENT_GIVENNAME");

				// Patient Gender - Values will be "M" or "F"
				String patientGender = nextRecord.get("PATIENT_GENDER");

				// Create the Patient resource
				Patient patient = new Patient();
				patient.setId("Patient/" + patientId);
				patient.addName().setFamily(patientFamilyName).addGiven(patientGivenName);

				// Gender code needs to be mapped
				switch (patientGender) {
					case "M":
						patient.setGender(Enumerations.AdministrativeGender.MALE);
						break;
					case "F":
						patient.setGender(Enumerations.AdministrativeGender.FEMALE);
						break;
				}

				// Upload the patient resource using a client-assigned ID create
				client.update().resource(patient).execute();

				Patient patientRead = client.read().resource(Patient.class).withId(patientId).execute();

				// White blood cell count - This corresponds to LOINC code:
				// Code: 6690-2
				// Display: Leukocytes [#/volume] in Blood by Automated count
				// Unit System: http://unitsofmeasure.org
				// Unit Code: 10*3/uL
				String rbc = nextRecord.get("RBC");

				// Create the RBC Observation
				Observation rbcObservation = new Observation();
				rbcObservation.setId("Observation/rbc-" + seqN);
				rbcObservation.setStatus(Observation.ObservationStatus.FINAL);
				rbcObservation.setEffective(new DateTimeType(timestamp));
				Coding rbcCode = new Coding()
						.setSystem("http://loinc.org")
						.setCode("6690-2")
						.setDisplay("Leukocytes [#/volume] in Blood by Automated count");
				rbcObservation.getCode().addCoding(rbcCode);
				Quantity rbcValue = new SimpleQuantity()
						.setSystem("http://unitsofmeasure.org")
						.setUnit("10*3/uL")
						.setCode("10*3/uL")
						.setValue(new BigDecimal(rbc));
				rbcObservation.setValue(rbcValue);
				rbcObservation.setSubject(new Reference("Patient/" + patientId));

				// Upload the RBC Observation resource using a client-assigned ID create
				client.update().resource(rbcObservation).execute();

				// White blood cell count - This corresponds to LOINC code:
				// Code: 789-8
				// Display: Erythrocytes [#/volume] in Blood by Automated count
				// Unit System: http://unitsofmeasure.org
				// Unit Code: 10*6/uL
				String wbc = nextRecord.get("WBC");

				// Create the WBC Observation
				Observation wbcObservation = new Observation();
				wbcObservation.setId("Observation/wbc-" + seqN);
				wbcObservation.setStatus(Observation.ObservationStatus.FINAL);
				wbcObservation.setEffective(new DateTimeType(timestamp));
				Coding wbcCode = new Coding()
						.setSystem("http://loinc.org")
						.setCode("789-8")
						.setDisplay("Erythrocytes [#/volume] in Blood by Automated count");
				wbcObservation.getCode().addCoding(wbcCode);
				Quantity wbcValue = new SimpleQuantity()
						.setSystem("http://unitsofmeasure.org")
						.setUnit("10*6/uL")
						.setCode("10*6/uL")
						.setValue(new BigDecimal(wbc));
				wbcObservation.setValue(wbcValue);
				wbcObservation.setSubject(new Reference("Patient/" + patientId));

				// Upload the WBC Observation resource using a client-assigned ID create
				client.update().resource(wbcObservation).execute();

				// Hemoglobin
				// Code: 718-7
				// Display: Hemoglobin [Mass/volume] in Blood
				// Unit System: http://unitsofmeasure.org
				// Unit Code: g/dL
				String hb = nextRecord.get("HB");

				// Create the HB Observation
				Observation hbObservation = new Observation();
				hbObservation.setId("Observation/hb-" + seqN);
				hbObservation.setStatus(Observation.ObservationStatus.FINAL);
				hbObservation.setEffective(new DateTimeType(timestamp));
				Coding hbCode = new Coding()
						.setSystem("http://loinc.org")
						.setCode("718-7")
						.setDisplay("Hemoglobin [Mass/volume] in Blood");
				hbObservation.getCode().addCoding(hbCode);
				Quantity hbValue = new SimpleQuantity()
						.setSystem("http://unitsofmeasure.org")
						.setUnit("g/dL")
						.setCode("g/dL")
						.setValue(new BigDecimal(hb));
				hbObservation.setValue(hbValue);
				hbObservation.setSubject(new Reference("Patient/" + patientId));

				// Upload the HB Observation resource using a client-assigned ID create
				client.update().resource(hbObservation).execute();

			}
		}
	}
}
