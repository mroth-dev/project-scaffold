package com.example.scaffold.security;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.example.scaffold.order.ShoppingCart;

/**
 * Makes the current login state and cart size available to every Thymeleaf
 * template (nav bar login/logout links, cart badge, etc.) without pulling in
 * the Spring Security Thymeleaf dialect.
 *
 * <p>ShoppingCart is looked up via ObjectProvider rather than injected
 * directly: this advice is a {@code @ControllerAdvice}, so {@code @WebMvcTest}
 * slices always instantiate it, but they don't include plain {@code @Component}
 * beans like ShoppingCart - a hard constructor dependency would fail every
 * narrow controller test in the project, not just ones that touch the cart.
 */
@ControllerAdvice
public class WebAuthenticationModelAdvice {

    private final ObjectProvider<ShoppingCart> shoppingCart;

    public WebAuthenticationModelAdvice(ObjectProvider<ShoppingCart> shoppingCart) {
        this.shoppingCart = shoppingCart;
    }

    @ModelAttribute
    public void addAuthenticationAttributes(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean authenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);

        model.addAttribute("authenticated", authenticated);
        if (authenticated && authentication.getPrincipal() instanceof CustomUserDetailsService.CustomUserPrincipal principal) {
            model.addAttribute("currentUserEmail", principal.getUsername());
        }
        ShoppingCart cart = shoppingCart.getIfAvailable();
        model.addAttribute("cartItemCount", cart != null ? cart.getItemCount() : 0);
    }
}
