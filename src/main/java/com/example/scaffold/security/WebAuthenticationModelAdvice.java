package com.example.scaffold.security;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.example.scaffold.category.CategoryService;
import com.example.scaffold.category.CategoryTreeDto;
import com.example.scaffold.order.ShoppingCart;

/**
 * Makes the current login state, cart size, and category tree available to
 * every Thymeleaf template (nav bar login/logout links, cart badge, burger
 * menu, etc.) without pulling in the Spring Security Thymeleaf dialect.
 *
 * <p>ShoppingCart and CategoryService are looked up via ObjectProvider rather
 * than injected directly: this advice is a {@code @ControllerAdvice}, so
 * {@code @WebMvcTest} slices always instantiate it, but they don't include
 * plain {@code @Component}/{@code @Service} beans like these - a hard
 * constructor dependency would fail every narrow controller test in the
 * project, not just ones that touch the cart or categories.
 */
@ControllerAdvice
public class WebAuthenticationModelAdvice {

    private final ObjectProvider<ShoppingCart> shoppingCart;
    private final ObjectProvider<CategoryService> categoryService;

    public WebAuthenticationModelAdvice(ObjectProvider<ShoppingCart> shoppingCart,
            ObjectProvider<CategoryService> categoryService) {
        this.shoppingCart = shoppingCart;
        this.categoryService = categoryService;
    }

    @ModelAttribute
    public void addAuthenticationAttributes(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean authenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);

        model.addAttribute("authenticated", authenticated);
        boolean isAdminOrManager = false;
        if (authenticated) {
            if (authentication.getPrincipal() instanceof CustomUserDetailsService.CustomUserPrincipal principal) {
                model.addAttribute("currentUserEmail", principal.getUsername());
            }
            isAdminOrManager = hasAnyRole(authentication, "ROLE_ADMIN", "ROLE_MANAGER");
        }
        model.addAttribute("isAdminOrManager", isAdminOrManager);
        ShoppingCart cart = shoppingCart.getIfAvailable();
        model.addAttribute("cartItemCount", cart != null ? cart.getItemCount() : 0);

        CategoryService categories = categoryService.getIfAvailable();
        model.addAttribute("navCategories", categories != null ? categories.getCategoryTree() : List.<CategoryTreeDto>of());
    }

    private boolean hasAnyRole(Authentication authentication, String... roles) {
        Set<String> wanted = Set.of(roles);
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (wanted.contains(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
