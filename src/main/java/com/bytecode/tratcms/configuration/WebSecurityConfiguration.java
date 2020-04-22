package com.bytecode.tratcms.configuration;

import com.bytecode.tratcms.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
@EnableWebSecurity
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {
    @Autowired
    private UsuarioService usuarioService;

    String[] resources = new String[]{
            "/include/**","/css/**","/icons/**","/img/**","/js/**","/vendor/**"
    };


    @Bean
    public BCryptPasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(usuarioService)
            .passwordEncoder(passwordEncoder());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
           http.authorizeRequests()
				.antMatchers(resources).permitAll()
				.anyRequest().authenticated()
				.and()
				.formLogin()
				.loginPage("/login")
				.permitAll()
                   .successForwardUrl("/admin/")
				.defaultSuccessUrl("/admin/")
				.failureUrl("/login?error=true")
				.usernameParameter("username")
				.passwordParameter("password")
				.and()
				.logout()
				.permitAll()
				.logoutSuccessUrl("/login?logout")
				.and()
				.rememberMe().key("uniqueAndSecret");
    }
}
