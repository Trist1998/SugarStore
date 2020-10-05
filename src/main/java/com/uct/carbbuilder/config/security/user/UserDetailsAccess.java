package com.uct.carbbuilder.config.security.user;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public interface UserDetailsAccess
{
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
}