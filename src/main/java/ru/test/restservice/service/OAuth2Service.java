package ru.test.restservice.service;

import com.owlike.genson.Genson;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import ru.test.restservice.config.OAuth2Properties;
import ru.test.restservice.dao.UserRepository;
import ru.test.restservice.entity.User;
import ru.test.restservice.exceptions.GenericException;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
/**
 * Класс, завершающий авторизацию через OAuth
 */
@Service
@RequiredArgsConstructor
public class OAuth2Service {

    private final UserRepository userRepository;
    private final OAuth2Properties oAuth2Properties;
    /**
     * Метод, обращающийся к url CAS сервера
     *
     * @return JSON или html страницу
     */
    // TODO: Feign
    public String sendRequest(String URL) {
        try {
            URLConnection connection = new URL(URL).openConnection();
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            InputStream is = connection.getInputStream();
            InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
            char[] buffer = new char[256];
            int rc;

            StringBuilder sb = new StringBuilder();

            while ((rc = reader.read(buffer)) != -1)
                sb.append(buffer, 0, rc);

            reader.close();
            return sb.toString();

        } catch (Exception er) {
            return er.getMessage();
        }
    }

    /**
     * Получение информации о пользователе с CAS сервера
     * (оставляю только имя, но там ещё прилично полей)
     */
    public boolean getUserInfo(String code){
        try {
            String accessToken = sendRequest("https://cas.spbstu.ru/oauth2.0/accessToken?" +
                    "grant_type=authorization_code" +
                    "&client_id=" + oAuth2Properties.clientId +
                    "&client_secret=" + oAuth2Properties.clientSecret +
                    "&code=" + code + "" +
                    "&redirect_uri=https://latex.icst.spbstu.ru/callback");
            Map<String, String> tokenParams = new Genson().deserialize(accessToken, Map.class);

            String userInfo = sendRequest("https://cas.spbstu.ru/oauth2.0/profile?access_token=" + tokenParams.get("access_token"));

            Map<String, Object> userParams = new Genson().deserialize(userInfo, Map.class);

            String username = userParams.get("sAMAccountName").toString();

            User user;
            Optional<User> mbUser = userRepository.findByEmail(username);
            if (mbUser.isPresent()) {
                user = mbUser.get();
            } else {
                user = new User(username, "", "ROLE_USER", false);
                userRepository.save(user);
            }

            if (!user.banned) {
                Authentication authentication = new UsernamePasswordAuthenticationToken(username, "", Collections.singleton(new SimpleGrantedAuthority(user.role)));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                return true;
            } else {
                return false;
            }

        } catch (Exception er) {
            throw new GenericException(er.getMessage());
        }
    }
}
