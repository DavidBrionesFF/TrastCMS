package com.bytecode.tratcms.security.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.Date;

public class JwtUtil {

    // Método para crear el JWT y enviarlo al cliente en el header de la respuesta
    public static void addAuthentication(HttpServletResponse res, String username) {

        String token = Jwts.builder()
                .setSubject(username)

                // Vamos a asignar un tiempo de expiracion de 30 minuto
                // solo con fines demostrativos en el video que hay al final
                .setExpiration(new Date(System.currentTimeMillis() + (60000*100)))

                // Hash con el que firmaremos la clave
                .signWith(SignatureAlgorithm.HS512, "P@tit0")
                .compact();

        //agregamos al encabezado el token
        res.addHeader("Authorization",  token);
    }

    // Método para validar el token enviado por el cliente
    public static Authentication getAuthentication(HttpServletRequest request) {

        // Obtenemos el token que viene en el encabezado de la peticion
        String token = request.getHeader("Authorization");

        // si hay un token presente, entonces lo validamos
        if (token != null) {
            String user = Jwts.parser()
                    .setSigningKey("P@tit0")
                    .parseClaimsJws(token.replace("Bearer", token)) //este metodo es el que valida
                    .getBody()
                    .getSubject();

            // Recordamos que para las demás peticiones que no sean /auth
            // no requerimos una autenticacion por username/password
            // por este motivo podemos devolver un UsernamePasswordAuthenticationToken sin password
            return user != null ?
                    new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()) :
                    null;
        }
        return null;
    }
}
