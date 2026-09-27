package com.fiap.vinshare.support;

import com.fiap.vinshare.domain.entities.Analyst;
import com.fiap.vinshare.domain.entities.Customer;
import com.fiap.vinshare.domain.entities.CustomerSegment;
import com.fiap.vinshare.domain.entities.CustomerSegmentType;
import com.fiap.vinshare.domain.entities.Dealership;
import com.fiap.vinshare.domain.entities.LoyaltyAccount;
import com.fiap.vinshare.domain.entities.Reward;
import com.fiap.vinshare.domain.entities.User;
import com.fiap.vinshare.domain.entities.UserRole;
import com.fiap.vinshare.domain.entities.Vehicle;
import com.fiap.vinshare.domain.entities.Warranty;
import com.fiap.vinshare.domain.entities.WarrantyStatus;
import com.fiap.vinshare.infra.security.CryptoService;
import com.fiap.vinshare.infra.security.JwtService;
import com.fiap.vinshare.repositories.AnalystRepository;
import com.fiap.vinshare.repositories.CustomerRepository;
import com.fiap.vinshare.repositories.CustomerSegmentRepository;
import com.fiap.vinshare.repositories.DealershipRepository;
import com.fiap.vinshare.repositories.LoyaltyAccountRepository;
import com.fiap.vinshare.repositories.RewardRepository;
import com.fiap.vinshare.repositories.UserRepository;
import com.fiap.vinshare.repositories.VehicleRepository;
import com.fiap.vinshare.repositories.WarrantyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Cria dados de teste direto pelos repositórios (mais rápido que pela API).
 * Cada chamada gera registros novos, então os testes não dependem de ordem.
 */
@TestComponent
@RequiredArgsConstructor
public class TestFixtures {

    public static final String PASSWORD = "Senha-de-teste-123";

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final AnalystRepository analystRepository;
    private final DealershipRepository dealershipRepository;
    private final VehicleRepository vehicleRepository;
    private final WarrantyRepository warrantyRepository;
    private final CustomerSegmentRepository segmentRepository;
    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final RewardRepository rewardRepository;
    private final PasswordEncoder passwordEncoder;
    private final CryptoService cryptoService;
    private final JwtService jwtService;

    private String encodedPassword;

    public Dealership dealership() {
        return dealershipRepository.save(Dealership.builder()
                .name("Ford Teste " + UUID.randomUUID().toString().substring(0, 6))
                .address("Av. Teste, 100")
                .city("São Paulo").state("SP")
                .zipCode("01000-000")
                .lat(new BigDecimal("-23.5505")).lng(new BigDecimal("-46.6333"))
                .phone("+55 (11) 4000-0000")
                .openingHours("Seg-Sex 08:00-18:00")
                .build());
    }

    public User admin() {
        return userRepository.save(User.builder()
                .email(TestData.uniqueEmail("admin"))
                .displayName("Admin Teste")
                .passwordHash(encodedPassword())
                .role(UserRole.ADMIN)
                .build());
    }

    public User analyst(Dealership dealership) {
        User user = userRepository.save(User.builder()
                .email(TestData.uniqueEmail("analista"))
                .displayName("Analista Teste")
                .passwordHash(encodedPassword())
                .role(UserRole.ANALYST)
                .build());
        analystRepository.save(Analyst.builder()
                .user(user).dealership(dealership).fullName("Analista Teste").build());
        return user;
    }

    public Customer customer() {
        User user = userRepository.save(User.builder()
                .email(TestData.uniqueEmail("cliente"))
                .passwordHash(encodedPassword())
                .role(UserRole.CLIENT)
                .build());
        String cpf = TestData.randomCpf();
        Customer saved = customerRepository.save(Customer.builder()
                .user(user)
                .fullName("Cliente Teste")
                .cpfEncrypted(cryptoService.encrypt(cpf))
                .cpfLookupHash(cryptoService.hash(cpf))
                .phone("+5511999990000")
                .lgpdConsentAt(OffsetDateTime.now())
                .build());
        // Entidades com UUID pré-atribuído passam por merge: o retorno traz o User
        // como proxy lazy, inutilizável fora da transação. Reanexa o objeto concreto.
        saved.setUser(user);
        return saved;
    }

    public Customer customer(Dealership homeDealership) {
        Customer customer = customer();
        User user = customer.getUser();
        customer.setHomeDealership(homeDealership);
        Customer saved = customerRepository.save(customer);
        saved.setUser(user);   // mesmo cuidado de customer(): o merge devolve o User como proxy lazy
        return saved;
    }

    public Vehicle vehicle(Customer customer) {
        return vehicleRepository.save(Vehicle.builder()
                .customer(customer)
                .model("Ranger").version("XLT 3.2").year((short) 2023)
                .plate(TestData.randomPlate()).vin(TestData.randomVin())
                .currentKm(15_000)
                .build());
    }

    public Warranty warranty(Vehicle vehicle) {
        return warrantyRepository.save(Warranty.builder()
                .vehicle(vehicle)
                .startDate(LocalDate.now().minusYears(1))
                .endDate(LocalDate.now().plusYears(2))
                .status(WarrantyStatus.ACTIVE)
                .build());
    }

    public CustomerSegment segment(Customer customer, CustomerSegmentType type, int risk) {
        return segmentRepository.save(CustomerSegment.builder()
                .customer(customer)
                .segment(type)
                .riskScore(BigDecimal.valueOf(risk).setScale(2))
                .modelVersion("teste-v1")
                .predictedAt(OffsetDateTime.now())
                .build());
    }

    public Reward reward(int pointsCost) {
        return rewardRepository.save(Reward.builder()
                .name("Prêmio teste " + UUID.randomUUID().toString().substring(0, 6))
                .pointsCost(pointsCost)
                .build());
    }

    public LoyaltyAccount setBalance(Customer customer, int points) {
        LoyaltyAccount account = loyaltyAccountRepository.findByCustomerId(customer.getId())
                .orElseGet(() -> LoyaltyAccount.builder().customer(customer).build());
        account.setBalance(points);
        return loyaltyAccountRepository.save(account);
    }

    public String tokenFor(User user) {
        return jwtService.generateAccessToken(user);
    }

    private String encodedPassword() {
        // BCrypt custo 12 leva ~250 ms: calcula uma única vez por contexto.
        if (encodedPassword == null) encodedPassword = passwordEncoder.encode(PASSWORD);
        return encodedPassword;
    }
}
