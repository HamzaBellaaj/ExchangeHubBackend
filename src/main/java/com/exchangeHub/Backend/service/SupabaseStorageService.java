package com.exchangeHub.Backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SupabaseStorageService {

    private static final Logger logger = LoggerFactory.getLogger(SupabaseStorageService.class);

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-role-key}")
    private String serviceRoleKey;

    @Value("${supabase.storage.bucket}")
    private String bucket;

    private final RestTemplate restTemplate;

    public SupabaseStorageService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String uploadFile(MultipartFile file, String storagePath) {
        // Vérifications préalables
        if (file == null) {
            throw new RuntimeException("Le fichier est null");
        }
        if (file.isEmpty()) {
            throw new RuntimeException("Le fichier est vide");
        }

        // Vérifier les propriétés Supabase
        if (supabaseUrl == null || supabaseUrl.isEmpty()) {
            throw new RuntimeException("supabase.url n'est pas configuré");
        }
        if (serviceRoleKey == null || serviceRoleKey.isEmpty()) {
            throw new RuntimeException("supabase.service-role-key n'est pas configuré");
        }
        if (bucket == null || bucket.isEmpty()) {
            throw new RuntimeException("supabase.storage.bucket n'est pas configuré");
        }

        // Construire l'URL Supabase Storage
        String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + storagePath;

        // Logs détaillés
        logger.info("========== START UPLOAD TO SUPABASE ==========");
        logger.info("URL: {}", uploadUrl);
        logger.info("Bucket: {}", bucket);
        logger.info("StoragePath: {}", storagePath);
        logger.info("File size: {} bytes", file.getSize());
        logger.info("Content-Type: {}", file.getContentType());

        // Préparer les headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + serviceRoleKey);
        headers.set("apikey", serviceRoleKey);
        // Si contentType est null, utiliser application/octet-stream
        String contentType = file.getContentType();
        if (contentType == null || contentType.isEmpty()) {
            contentType = "application/octet-stream";
        }
        headers.set("Content-Type", contentType);
        headers.set("x-upsert", "false");

        try {
            // Envoyer les bytes bruts du fichier (pas de multipart)
            org.springframework.http.HttpEntity<byte[]> request = 
                new org.springframework.http.HttpEntity<>(file.getBytes(), headers);

            logger.info("Sending POST request to Supabase...");
            ResponseEntity<String> response = restTemplate.postForEntity(uploadUrl, request, String.class);

            // Vérifier le statut de réponse
            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.error("Upload failed - Status: {}, Body: {}",
                    response.getStatusCode(), response.getBody());
                throw new RuntimeException("Erreur upload Supabase Storage - Status: " + response.getStatusCode() +
                    " - " + response.getBody());
            }

            logger.info("Upload successful - Status: {}", response.getStatusCode());
            logger.info("File URL: {}/storage/v1/object/{}/{}", supabaseUrl, bucket, storagePath);
            logger.info("========== UPLOAD COMPLETED SUCCESSFULLY ==========");

            return storagePath;

        } catch (HttpClientErrorException e) {
            logger.error("HTTP Client Error during upload - Status: {}, Body: {}",
                e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException("Erreur upload Supabase Storage - " + e.getStatusCode() +
                " - " + e.getResponseBodyAsString());

        } catch (HttpServerErrorException e) {
            logger.error("HTTP Server Error during upload - Status: {}, Body: {}",
                e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException("Erreur upload Supabase Storage - " + e.getStatusCode() +
                " - " + e.getResponseBodyAsString());

        } catch (ResourceAccessException e) {
            logger.error("Timeout or network error to Supabase Storage: {}", e.getMessage(), e);
            throw new RuntimeException("Timeout ou problème réseau vers Supabase Storage: " + e.getMessage());

        } catch (Exception e) {
            logger.error("Unexpected error during file upload: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur upload fichier: " + e.getMessage());
        }
    }

    public String createSignedUrl(String storagePath, int expiresInSeconds) {
        // Vérifier storagePath non null/non vide
        if (storagePath == null || storagePath.isEmpty()) {
            throw new RuntimeException("storagePath est null ou vide");
        }

        // Construire l'URL Supabase
        String signUrl = supabaseUrl + "/storage/v1/object/sign/" + bucket + "/" + storagePath;

        logger.info("========== GENERATING SIGNED URL ==========");
        logger.info("SignURL endpoint: {}", signUrl);
        logger.info("StoragePath: {}", storagePath);
        logger.info("ExpiresIn: {} seconds", expiresInSeconds);

        // Préparer les headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + serviceRoleKey);
        headers.set("apikey", serviceRoleKey);
        headers.set("Content-Type", "application/json");

        // Construire le body JSON
        String jsonBody = "{\"expiresIn\": " + expiresInSeconds + "}";

        try {
            // Préparer la requête
            org.springframework.http.HttpEntity<String> request = 
                new org.springframework.http.HttpEntity<>(jsonBody, headers);

            logger.info("Sending POST request to Supabase sign endpoint...");
            ResponseEntity<String> response = restTemplate.postForEntity(signUrl, request, String.class);

            // Vérifier le statut de réponse
            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.error("Sign URL generation failed - Status: {}, Body: {}",
                    response.getStatusCode(), response.getBody());
                throw new RuntimeException("Erreur génération signed URL Supabase - Status: " + response.getStatusCode() +
                    " - " + response.getBody());
            }

            // Extraire signedURL du JSON
            String responseBody = response.getBody();
            logger.info("Signed URL response: {}", responseBody);

            // Simple extraction du signedURL depuis le JSON
            String signedURL = extractSignedUrlFromJson(responseBody);
            if (signedURL == null || signedURL.isEmpty()) {
                logger.error("signedURL not found in Supabase response");
                throw new RuntimeException("signedURL absent de la réponse Supabase");
            }

            // Construire l'URL finale
            String finalUrl;
            if (signedURL.startsWith("/")) {
                finalUrl = supabaseUrl + signedURL;
            } else {
                finalUrl = signedURL;
            }

            logger.info("Signed URL generated successfully");
            logger.info("Final URL: {}", finalUrl);
            logger.info("========== SIGNED URL GENERATED ==========");

            return finalUrl;

        } catch (HttpClientErrorException e) {
            logger.error("HTTP Client Error during sign URL generation - Status: {}, Body: {}",
                e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException("Erreur génération signed URL - " + e.getStatusCode() +
                " - " + e.getResponseBodyAsString());

        } catch (HttpServerErrorException e) {
            logger.error("HTTP Server Error during sign URL generation - Status: {}, Body: {}",
                e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException("Erreur génération signed URL - " + e.getStatusCode() +
                " - " + e.getResponseBodyAsString());

        } catch (ResourceAccessException e) {
            logger.error("Timeout or network error during sign URL generation: {}", e.getMessage(), e);
            throw new RuntimeException("Timeout ou problème réseau vers Supabase Storage: " + e.getMessage());

        } catch (Exception e) {
            logger.error("Unexpected error during sign URL generation: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur génération signed URL: " + e.getMessage());
        }
    }

    public String generateSignedUrl(String storagePath, int expiresInSeconds) {
        return createSignedUrl(storagePath, expiresInSeconds);
    }

    private String extractSignedUrlFromJson(String jsonResponse) {
        // Simple extraction du signedURL depuis le JSON
        // Format: {"signedURL":"/storage/v1/object/sign/..."}
        if (jsonResponse == null || jsonResponse.isEmpty()) {
            return null;
        }

        int signedUrlIndex = jsonResponse.indexOf("\"signedURL\"");
        if (signedUrlIndex == -1) {
            return null;
        }

        int colonIndex = jsonResponse.indexOf(":", signedUrlIndex);
        if (colonIndex == -1) {
            return null;
        }

        int firstQuote = jsonResponse.indexOf("\"", colonIndex);
        if (firstQuote == -1) {
            return null;
        }

        int secondQuote = jsonResponse.indexOf("\"", firstQuote + 1);
        if (secondQuote == -1) {
            return null;
        }

        return jsonResponse.substring(firstQuote + 1, secondQuote);
    }
}
