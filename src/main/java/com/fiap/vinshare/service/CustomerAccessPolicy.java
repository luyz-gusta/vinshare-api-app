package com.fiap.vinshare.service;

import com.fiap.vinshare.domain.entities.Customer;
import com.fiap.vinshare.domain.entities.Dealership;
import com.fiap.vinshare.domain.entities.User;
import com.fiap.vinshare.infra.errors.exceptions.ResourceNotFoundException;
import com.fiap.vinshare.repositories.AnalystRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Escopo de acesso a dados pessoais de clientes (OWASP API1 - BOLA).
 * ADMIN enxerga a rede inteira; ANALYST só os clientes cuja concessionária de
 * relacionamento é a dele; CLIENT não acessa dados de outros clientes.
 */
@Component
@RequiredArgsConstructor
public class CustomerAccessPolicy {

    private final AnalystRepository analystRepository;

    /** Parâmetros de escopo usados nas consultas de lista. */
    public record Scope(boolean allDealerships, String dealershipId) {

        private static final String NONE = "00000000-0000-0000-0000-000000000000";

        static Scope all() {
            return new Scope(true, NONE);
        }

        static Scope of(UUID dealershipId) {
            return new Scope(false, dealershipId.toString());
        }
    }

    public Scope scopeFor(User user) {
        return switch (user.getRole()) {
            case ADMIN -> Scope.all();
            case ANALYST -> analystRepository.findByUser(user)
                    .map(a -> Scope.of(a.getDealership().getId()))
                    .orElseThrow(() -> new AccessDeniedException("Analista sem concessionária vinculada"));
            default -> throw new AccessDeniedException("Perfil sem acesso a dados de clientes");
        };
    }

    /**
     * Responde 404 (e não 403) quando o cliente está fora do escopo, para não
     * revelar a existência de clientes de outras concessionárias.
     */
    public void checkAccess(User user, Customer customer) {
        Scope scope = scopeFor(user);
        if (scope.allDealerships()) return;
        Dealership home = customer.getHomeDealership();
        if (home == null || !home.getId().toString().equals(scope.dealershipId())) {
            throw ResourceNotFoundException.of("Cliente", customer.getId());
        }
    }
}
