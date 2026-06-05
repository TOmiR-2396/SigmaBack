package com.example.gym.service;

import com.example.gym.dto.GoogleSheetsPreviewResponse;
import com.example.gym.model.PaymentRecord;
import com.example.gym.model.TrainingPlan;
import com.example.gym.model.User;
import com.example.gym.repository.PaymentRecordRepository;
import com.example.gym.repository.UserRepository;
import com.example.gym.tenant.TenantContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GoogleSheetsService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleSheetsService.class);
    private static final String SHEETS_API_BASE = "https://sheets.googleapis.com/v4/spreadsheets";
    private static final String OAUTH_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ofPattern("d/M/yyyy"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("M/d/yyyy"),
    };

    @Value("${google.client-id:}")
    private String clientId;

    @Value("${google.client-secret:}")
    private String clientSecret;

    @Autowired
    private GoogleSheetsCredentialService credentialService;

    @Autowired
    private PaymentRecordRepository paymentRecordRepository;

    @Autowired
    private UserRepository userRepository;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    // ==================== TOKEN MANAGEMENT ====================

    private String ensureValidToken(String tenantId) throws Exception {
        String token = credentialService.getAccessToken();
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("No hay access token de Google Sheets configurado");
        }
        // Probar con una request simple; si 401, refrescar
        // Para optimizar, podríamos guardar expiry. Por ahora, intentamos refresh en 401.
        return token;
    }

    private String refreshAccessToken(String tenantId) throws Exception {
        String refreshToken = credentialService.getRefreshToken();
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalStateException("No hay refresh token disponible");
        }

        String body = "grant_type=refresh_token"
                + "&refresh_token=" + urlEncode(refreshToken)
                + "&client_id=" + urlEncode(clientId)
                + "&client_secret=" + urlEncode(clientSecret);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(OAUTH_TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
            Map<String, Object> json = mapper.readValue(resp.body(), new TypeReference<>() {});
            String newAccessToken = String.valueOf(json.get("access_token"));
            credentialService.updateAccessToken(tenantId, newAccessToken);
            logger.info("[Google Sheets] Access token refrescado para tenant={}", tenantId);
            return newAccessToken;
        } else {
            logger.error("[Google Sheets] Error refrescando token: status={} body={}", resp.statusCode(), resp.body());
            throw new RuntimeException("No se pudo refrescar el token de Google");
        }
    }

    // ==================== SHEETS API RAW ====================

    public List<List<Object>> readSheet(String spreadsheetId, String range) throws Exception {
        String tenantId = TenantContext.getCurrentTenant();
        String token = ensureValidToken(tenantId);

        String url = SHEETS_API_BASE + "/" + spreadsheetId + "/values/" + urlEncode(range);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (resp.statusCode() == 401) {
            token = refreshAccessToken(tenantId);
            req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        }

        if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
            Map<String, Object> json = mapper.readValue(resp.body(), new TypeReference<>() {});
            Object values = json.get("values");
            if (values == null) return Collections.emptyList();
            @SuppressWarnings("unchecked")
            List<List<Object>> rows = (List<List<Object>>) values;
            return rows;
        } else {
            logger.error("[Google Sheets] Error leyendo sheet: status={} body={}", resp.statusCode(), resp.body());
            throw new RuntimeException("Error leyendo Google Sheet: " + resp.body());
        }
    }

    public void writeSheet(String spreadsheetId, String range, List<List<Object>> values) throws Exception {
        String tenantId = TenantContext.getCurrentTenant();
        String token = ensureValidToken(tenantId);

        String url = SHEETS_API_BASE + "/" + spreadsheetId + "/values/" + urlEncode(range)
                + "?valueInputOption=RAW";

        Map<String, Object> body = Map.of("values", values);
        String jsonBody = mapper.writeValueAsString(body);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .method("PUT", HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (resp.statusCode() == 401) {
            token = refreshAccessToken(tenantId);
            req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .method("PUT", HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();
            resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        }

        if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
            logger.info("[Google Sheets] Escrito correctamente range={}", range);
        } else {
            logger.error("[Google Sheets] Error escribiendo sheet: status={} body={}", resp.statusCode(), resp.body());
            throw new RuntimeException("Error escribiendo Google Sheet: " + resp.body());
        }
    }

    // ==================== PREVIEW ====================

    public GoogleSheetsPreviewResponse previewPayments(String spreadsheetId) throws Exception {
        List<List<Object>> raw = readSheet(spreadsheetId, "A1:F20");
        return buildPreview("payments", raw, Arrays.asList("APELLIDO", "NOMBRES", "MONTO TOTAL", "FORMA DE PAGO", "FECHA DE PAGO", "OBSERVACIONES"));
    }

    public GoogleSheetsPreviewResponse previewPlans(String spreadsheetId) throws Exception {
        List<List<Object>> raw = readSheet(spreadsheetId, "A1:E20");
        return buildPreview("plans", raw, Arrays.asList("APELLIDO", "NOMBRE", "PLAN", "LINK", "NOTAS"));
    }

    private GoogleSheetsPreviewResponse buildPreview(String type, List<List<Object>> raw, List<String> expectedHeaders) {
        GoogleSheetsPreviewResponse resp = new GoogleSheetsPreviewResponse();
        resp.setSheetType(type);
        resp.setWarnings(new ArrayList<>());

        if (raw.isEmpty()) {
            resp.setHeaders(expectedHeaders);
            resp.setRows(Collections.emptyList());
            resp.getWarnings().add("La hoja parece estar vacía. Se esperan columnas: " + String.join(", ", expectedHeaders));
            return resp;
        }

        List<String> headers = raw.get(0).stream().map(Object::toString).map(String::trim).collect(Collectors.toList());
        resp.setHeaders(headers);

        // Verificar headers esperados
        Set<String> headerSet = headers.stream().map(String::toLowerCase).collect(Collectors.toSet());
        for (String expected : expectedHeaders) {
            if (!headerSet.contains(expected.toLowerCase())) {
                resp.getWarnings().add("Columna esperada no encontrada: '" + expected + "'");
            }
        }

        List<Map<String, String>> rows = new ArrayList<>();
        for (int i = 1; i < raw.size() && i <= 6; i++) {
            List<Object> rawRow = raw.get(i);
            Map<String, String> row = new LinkedHashMap<>();
            for (int j = 0; j < headers.size(); j++) {
                String val = j < rawRow.size() ? rawRow.get(j).toString().trim() : "";
                row.put(headers.get(j), val);
            }
            rows.add(row);
        }
        resp.setRows(rows);
        return resp;
    }

    // ==================== SYNC PAYMENTS: DB → SHEETS ====================

    @Transactional(readOnly = true)
    public Map<String, Object> syncPaymentsToSheet(String spreadsheetId) throws Exception {
        String tenantId = TenantContext.getCurrentTenant();
        List<User> members = userRepository.findByTenantIdAndRole(tenantId, User.UserRole.MEMBER);

        // Obtener todos los pagos del tenant (aproximación: por usuarios del tenant)
        List<PaymentRecord> allPayments = new ArrayList<>();
        for (User u : members) {
            allPayments.addAll(paymentRecordRepository.findByUserIdOrderByCreatedAtDesc(u.getId()));
        }

        // Construir valores: header + rows
        List<List<Object>> values = new ArrayList<>();
        values.add(Arrays.asList("APELLIDO", "NOMBRES", "MONTO TOTAL", "FORMA DE PAGO", "FECHA DE PAGO", "OBSERVACIONES"));

        // Agrupar por mes actual (podría parametrizarse)
        LocalDate now = LocalDate.now();
        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear();

        int exported = 0;
        for (PaymentRecord p : allPayments) {
            LocalDate payDate = p.getPaymentDate() != null ? p.getPaymentDate() : p.getCreatedAt().toLocalDate();
            // Por defecto exportar solo el mes actual; se puede parametrizar luego
            if (payDate.getMonthValue() == currentMonth && payDate.getYear() == currentYear) {
                User u = p.getUser();
                values.add(Arrays.asList(
                        u.getLastName(),
                        u.getFirstName(),
                        String.valueOf(p.getAmount()),
                        p.getMethod().name(),
                        payDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        p.getNotes() != null ? p.getNotes() : ""
                ));
                exported++;
            }
        }

        // Append-only: leer cuántas filas hay y escribir a partir de la siguiente
        // Para simplificar, sobreescribimos la hoja completa (A1:F{N})
        String range = "A1:F" + values.size();
        writeSheet(spreadsheetId, range, values);

        Map<String, Object> result = new HashMap<>();
        result.put("exported", exported);
        result.put("message", "Exportados " + exported + " pagos del mes " + currentMonth + "/" + currentYear);
        return result;
    }

    // ==================== IMPORT PAYMENTS: SHEETS → DB ====================

    @Transactional
    public Map<String, Object> importPaymentsFromSheet(String spreadsheetId) throws Exception {
        List<List<Object>> raw = readSheet(spreadsheetId, "A1:F1000");
        String tenantId = TenantContext.getCurrentTenant();

        if (raw.size() < 2) {
            return Map.of("imported", 0, "message", "La hoja está vacía o solo tiene headers");
        }

        List<String> headers = raw.get(0).stream().map(Object::toString).map(String::trim).collect(Collectors.toList());
        int idxApellido = findColumnIndex(headers, "APELLIDO");
        int idxNombre = findColumnIndex(headers, "NOMBRES");
        int idxMonto = findColumnIndex(headers, "MONTO");
        int idxForma = findColumnIndex(headers, "FORMA");
        int idxFecha = findColumnIndex(headers, "FECHA");
        int idxObs = findColumnIndex(headers, "OBS");

        if (idxApellido == -1 && idxNombre == -1) {
            throw new IllegalArgumentException("No se encontró columna APELLIDO ni NOMBRES");
        }

        int imported = 0;
        int skipped = 0;
        List<String> warnings = new ArrayList<>();

        for (int i = 1; i < raw.size(); i++) {
            List<Object> row = raw.get(i);
            if (row.isEmpty()) continue;

            String apellido = idxApellido >= 0 && idxApellido < row.size() ? row.get(idxApellido).toString().trim() : "";
            String nombre = idxNombre >= 0 && idxNombre < row.size() ? row.get(idxNombre).toString().trim() : "";
            String montoStr = idxMonto >= 0 && idxMonto < row.size() ? row.get(idxMonto).toString().trim() : "";
            String forma = idxForma >= 0 && idxForma < row.size() ? row.get(idxForma).toString().trim() : "EF";
            String fechaStr = idxFecha >= 0 && idxFecha < row.size() ? row.get(idxFecha).toString().trim() : "";
            String obs = idxObs >= 0 && idxObs < row.size() ? row.get(idxObs).toString().trim() : "";

            if (apellido.isBlank() && nombre.isBlank()) continue;
            if (montoStr.isBlank()) {
                skipped++;
                continue; // fila vacía o pendiente
            }

            // Buscar usuario
            List<User> candidates = userRepository.findByNameLike(tenantId, apellido.isBlank() ? nombre : apellido);
            if (candidates.isEmpty()) {
                warnings.add("Fila " + (i + 1) + ": usuario no encontrado: " + apellido + ", " + nombre);
                continue;
            }
            User user = candidates.get(0); // Tomar el primero; se podría mejorar con fuzzy match

            double monto;
            try {
                montoStr = montoStr.replace("$", "").replace(".", "").replace(",", ".").trim();
                monto = Double.parseDouble(montoStr);
            } catch (NumberFormatException e) {
                warnings.add("Fila " + (i + 1) + ": monto inválido: " + montoStr);
                continue;
            }

            LocalDate fechaRaw = parseDate(fechaStr);
            final LocalDate fecha = fechaRaw != null ? fechaRaw : LocalDate.now();

            PaymentRecord.PaymentMethod method = parseMethod(forma);

            // Evitar duplicados: mismo usuario + misma fecha + mismo monto
            boolean exists = paymentRecordRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                    .anyMatch(r -> {
                        LocalDate d = r.getPaymentDate() != null ? r.getPaymentDate() : r.getCreatedAt().toLocalDate();
                        return Math.abs(r.getAmount() - monto) < 0.01 && d.equals(fecha);
                    });

            if (!exists) {
                PaymentRecord record = PaymentRecord.builder()
                        .user(user)
                        .amount(monto)
                        .method(method)
                        .paymentDate(fecha)
                        .notes(obs)
                        .status(PaymentRecord.PaymentStatus.APPROVED)
                        .build();
                record.setTenantId(tenantId);
                paymentRecordRepository.save(record);
                imported++;
            } else {
                skipped++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("imported", imported);
        result.put("skipped", skipped);
        result.put("warnings", warnings);
        result.put("message", "Importados " + imported + " pagos, " + skipped + " omitidos");
        return result;
    }

    // ==================== SYNC PLANS: DB → SHEETS ====================

    @Transactional(readOnly = true)
    public Map<String, Object> syncPlansToSheet(String spreadsheetId) throws Exception {
        String tenantId = TenantContext.getCurrentTenant();
        List<User> members = userRepository.findByTenantIdAndRole(tenantId, User.UserRole.MEMBER);

        List<List<Object>> values = new ArrayList<>();
        values.add(Arrays.asList("APELLIDO", "NOMBRE", "PLAN", "LINK", "NOTAS"));

        int exported = 0;
        for (User u : members) {
            String planName = "";
            String link = u.getTrainingPlanUrl() != null ? u.getTrainingPlanUrl() : "";

            // Buscar si tiene un TrainingPlan asignado
            // Nota: TrainingPlan.user es LAZY; para evitar N+1, podríamos necesitar un query custom.
            // Por simplicidad, usamos el trainingPlanUrl del usuario.
            values.add(Arrays.asList(
                    u.getLastName(),
                    u.getFirstName(),
                    planName,
                    link,
                    ""
            ));
            exported++;
        }

        String range = "A1:E" + values.size();
        writeSheet(spreadsheetId, range, values);

        return Map.of("exported", exported, "message", "Exportados " + exported + " planes");
    }

    // ==================== IMPORT PLANS: SHEETS → DB ====================

    @Transactional
    public Map<String, Object> importPlansFromSheet(String spreadsheetId) throws Exception {
        List<List<Object>> raw = readSheet(spreadsheetId, "A1:E1000");
        String tenantId = TenantContext.getCurrentTenant();

        if (raw.size() < 2) {
            return Map.of("imported", 0, "message", "La hoja está vacía");
        }

        List<String> headers = raw.get(0).stream().map(Object::toString).map(String::trim).collect(Collectors.toList());
        int idxApellido = findColumnIndex(headers, "APELLIDO");
        int idxNombre = findColumnIndex(headers, "NOMBRE");
        int idxPlan = findColumnIndex(headers, "PLAN");
        int idxLink = findColumnIndex(headers, "LINK");
        int idxNotas = findColumnIndex(headers, "NOTAS");

        int imported = 0;
        List<String> warnings = new ArrayList<>();

        for (int i = 1; i < raw.size(); i++) {
            List<Object> row = raw.get(i);
            if (row.isEmpty()) continue;

            String apellido = idxApellido >= 0 && idxApellido < row.size() ? row.get(idxApellido).toString().trim() : "";
            String nombre = idxNombre >= 0 && idxNombre < row.size() ? row.get(idxNombre).toString().trim() : "";
            String link = idxLink >= 0 && idxLink < row.size() ? row.get(idxLink).toString().trim() : "";

            if (apellido.isBlank() && nombre.isBlank()) continue;

            List<User> candidates = userRepository.findByNameLike(tenantId, apellido.isBlank() ? nombre : apellido);
            if (candidates.isEmpty()) {
                warnings.add("Fila " + (i + 1) + ": usuario no encontrado");
                continue;
            }

            User user = candidates.get(0);
            if (!link.isBlank()) {
                user.setTrainingPlanUrl(link);
                userRepository.save(user);
                imported++;
            }
        }

        return Map.of("imported", imported, "warnings", warnings, "message", "Actualizados " + imported + " links de planes");
    }

    // ==================== HELPERS ====================

    private int findColumnIndex(List<String> headers, String keyword) {
        for (int i = 0; i < headers.size(); i++) {
            String h = headers.get(i).toLowerCase();
            if (h.contains(keyword.toLowerCase())) return i;
        }
        return -1;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        value = value.trim();
        for (DateTimeFormatter fmt : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(value, fmt);
            } catch (DateTimeParseException ignored) {}
        }
        // Intentar parsear como número de serie de Excel (días desde 1900)
        try {
            int excelDays = Integer.parseInt(value.split("\\.")[0]);
            return LocalDate.of(1899, 12, 30).plusDays(excelDays);
        } catch (Exception ignored) {}
        return null;
    }

    private PaymentRecord.PaymentMethod parseMethod(String value) {
        if (value == null) return PaymentRecord.PaymentMethod.CASH;
        String v = value.toUpperCase().trim();
        if (v.startsWith("EF") || v.equals("E")) return PaymentRecord.PaymentMethod.CASH;
        if (v.contains("TRANS") || v.equals("T")) return PaymentRecord.PaymentMethod.TRANSFER;
        if (v.contains("MP") || v.contains("MERCADO")) return PaymentRecord.PaymentMethod.MP;
        return PaymentRecord.PaymentMethod.CASH;
    }

    private String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
