package web.controler;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import web.services.JwtService;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/spotify")
@RequiredArgsConstructor
public class SpotifyAuthController {

    @Value("${spotify.client-id}")
    private String clientId;

    @Value("${spotify.client-secret}")
    private String clientSecret;

    @Value("${spotify.redirect-uri}")
    private String redirectUri;

    private final JwtService jwtService; // Pour générer le JWT

    private static final String AUTH_URL = "https://accounts.spotify.com/authorize";
    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    private static final String USER_INFO_URL = "https://api.spotify.com/v1/me";

    private String spotifyAccessToken;

    @GetMapping("/login")
    public ResponseEntity<?> login() {
        String url = AUTH_URL + "?client_id=" + clientId +
                "&response_type=code&redirect_uri=" + redirectUri +
                "&scope=user-read-email user-read-private";
        return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, url).build();
    }

    @GetMapping("/callback")
    public ResponseEntity<?> callback(@RequestParam("code") String code) {
        RestTemplate restTemplate = new RestTemplate();

        // Échange du code contre un token d'accès
        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("grant_type", "authorization_code");
        requestBody.add("code", code);
        requestBody.add("redirect_uri", redirectUri);
        requestBody.add("client_id", clientId);
        requestBody.add("client_secret", clientSecret);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.exchange(TOKEN_URL, HttpMethod.POST, request, Map.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Erreur d'authentification");
        }

        spotifyAccessToken = (String) response.getBody().get("access_token");

        // Récupération des infos utilisateur
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(spotifyAccessToken);

        HttpEntity<String> entity = new HttpEntity<>(authHeaders);
        ResponseEntity<Map> userInfoResponse = restTemplate.exchange(USER_INFO_URL, HttpMethod.GET, entity, Map.class);

        if (!userInfoResponse.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Impossible de récupérer l'utilisateur");
        }

        Map<String, Object> userInfo = userInfoResponse.getBody();

        // Création d'un JWT
        String jwt = jwtService.generateToken(userInfo.get("id").toString());

        // **Redirection vers le frontend avec le JWT en paramètre**
        String redirectUrl = "http://localhost:3000/dashboard?token=" + jwt;
        return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, redirectUrl).build();
    }

    @GetMapping("/me")
    public ResponseEntity<?> getUserInfo(@RequestHeader("Authorization") String token) {
        try {
            String jwt = token.substring(7);
            String userId = jwtService.extractUserId(jwt);
            System.out.println("getUserInfo: " + userId);


            // 🔹 Appel à l'API Spotify avec le bon token
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(spotifyAccessToken); // Utilisation du token Spotify (et non JWT)

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(USER_INFO_URL, HttpMethod.GET, entity, Map.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Impossible de récupérer les infos utilisateur");
            }

            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            System.out.println(e.toString());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Erreur lors de la récupération des infos utilisateur");
        }
    }



}

