/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cc.bliss.match3.service.gamemanager.config;

import bliss.lib.framework.common.LogUtil;
import bliss.lib.framework.util.StringUtils;
import cc.bliss.match3.service.gamemanager.constant.GameConstant;
import cc.bliss.match3.service.gamemanager.ent.common.SessionObj;
import cc.bliss.match3.service.gamemanager.ent.enums.TeleLogType;
import cc.bliss.match3.service.gamemanager.localqueue.GMLocalQueue;
import cc.bliss.match3.service.gamemanager.localqueue.cmd.TelegramLoggerCmd;
import cc.bliss.match3.service.gamemanager.service.system.UserDetailsServiceImpl;
import cc.bliss.match3.service.gamemanager.util.JwtUtils;
import cc.bliss.match3.service.gamemanager.util.UserActivityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Phong
 */
@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {

            final String requestTokenHeader = request.getHeader("Authorization");

            if (StringUtils.isEmpty(requestTokenHeader)) {
                chain.doFilter(request, response);
                return;
            }

            SessionObj userDetails = null;
            if (jwtUtils.validateJwtToken(requestTokenHeader)
                    && SecurityContextHolder.getContext().getAuthentication() == null) {
                // client authentication
                long userID = jwtUtils.getUserIdFromJwtToken(requestTokenHeader);
                userDetails = userDetailsService.loadUserById(userID);

                UserActivityUtil userActivityUtil = UserActivityUtil.getInstance();
                userActivityUtil.updateLastActivity(userID);

            } else if (requestTokenHeader.contentEquals(GameConstant.AUTHORIZE_SECRET_TOKEN)) {
                // server game authentication
                userDetails = userDetailsService.loadUserById(GameConstant.SERVER_ID);

            }

            // set authentication to context
            if (userDetails != null) {
                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                usernamePasswordAuthenticationToken
                        .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
            }
            chain.doFilter(request, response);
        } catch (Exception e) {
            GMLocalQueue.addQueue(new TelegramLoggerCmd(LogUtil.stackTrace(e), TeleLogType.EXCEPTION, JwtRequestFilter.class));
        }
    }

}
