package web.controler;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import web.services.JwtService;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/deezer")
@RequiredArgsConstructor
public class DeezerAuthController {

    @Value("${deezer.client-id}")
    private String clientId;

    @Value("${deezer.client-secret}")
    private String clientSecret;

    @Value("${deezer.redirect-uri}")
    private String redirectUri;

    private final JwtService jwtService;

    private static final String AUTH_URL = "https://connect.deezer.com/oauth/auth.php";
    private static final String TOKEN_URL = "https://connect.deezer.com/oauth/access_token.php";
    private static final String USER_INFO_URL = "https://api.deezer.com/user/me";

    private String deezerAccessToken;

    @GetMapping("/login")
    public ResponseEntity<?> login() {
        // Les permissions demandées (ajuste-les selon tes besoins)
        String perms = "basic_access,email";
        // Construire l'URL de redirection Deezer
        String url = AUTH_URL + "?app_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&perms=" + perms;
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, url)
                .build();
    }

    @GetMapping("/callback")
    public ResponseEntity<?> callback(@RequestParam("code") String code) {
        RestTemplate restTemplate = new RestTemplate();

        // Construction de l'URL pour récupérer le token d'accès
        // Deezer attend une requête GET avec les paramètres en query string
        String url = TOKEN_URL + "?app_id=" + clientId +
                "&secret=" + clientSecret +
                "&code=" + code +
                "&output=json"; // Pour obtenir la réponse en JSON

        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Erreur d'authentification Deezer");
        }

        // Récupération du token d'accès Deezer
        deezerAccessToken = (String) response.getBody().get("access_token");

        // Récupération des infos utilisateur depuis Deezer
        String userInfoUrl = USER_INFO_URL + "?access_token=" + deezerAccessToken;
        ResponseEntity<Map> userInfoResponse = restTemplate.getForEntity(userInfoUrl, Map.class);
        if (!userInfoResponse.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Impossible de récupérer l'utilisateur Deezer");
        }

        Map<String, Object> userInfo = userInfoResponse.getBody();

        // Création d'un JWT en utilisant l'ID utilisateur retourné par Deezer
        String jwt = jwtService.generateToken(userInfo.get("id").toString());

        // Redirection vers le frontend avec le JWT en paramètre
        String redirectUrl = "http://localhost:3000/dashboard?token=" + jwt;
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, redirectUrl)
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<?> getUserInfo(@RequestHeader("Authorization") String token) {
        try {
            // Extraction du JWT (en enlevant "Bearer ")
            String jwt = token.substring(7);
            String userId = jwtService.extractUserId(jwt);
            System.out.println("getUserInfo: " + userId);

            // Construction de l'URL pour récupérer les infos utilisateur depuis Deezer
            String userInfoUrl = USER_INFO_URL + "?access_token=" + deezerAccessToken;
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.getForEntity(userInfoUrl, Map.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Impossible de récupérer les infos utilisateur Deezer");
            }

            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            System.out.println(e.toString());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Erreur lors de la récupération des infos utilisateur");
        }
    }
}
