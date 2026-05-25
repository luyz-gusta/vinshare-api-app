package com.fiap.vinshare.infra.seed;

import com.fiap.vinshare.domain.entities.*;
import com.fiap.vinshare.infra.security.CryptoService;
import com.fiap.vinshare.repositories.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Popula o banco com um dataset sintético realista para demonstração do
 * Challenge Ford VIN Share. Ativado por `app.seed.enabled=true`.
 *
 * Idempotente: se já houver clientes cadastrados, não faz nada.
 *
 * Volumes:
 *  - 30 concessionárias (capitais e cidades médias brasileiras)
 *  - 5 prêmios de fidelidade
 *  - 1 OWNER (admin) + 15 analistas
 *  - 500 clientes (com usuário CLIENT e conta de fidelidade)
 *  - 500 veículos Ford + garantias (30% ativas, 40% vencidas, 30% vencendo)
 *  - ~1.500 serviços (a partir de agendamentos concluídos)
 *  - NPS, transações de fidelidade e segmentos de IA para a demo
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private static final String DEFAULT_PASSWORD = "senha123";
    private static final String[] MODELS = {"Ka", "Fiesta", "EcoSport", "Ranger", "Bronco", "Territory"};
    private static final String[] VERSIONS = {"SE 1.5", "Titanium 2.0", "XLT 3.2", "Raptor 3.0 V6", "Storm 2.0", "Limited 1.5T"};
    private static final String[] NPS_LIKED = {"ATENDIMENTO", "TEMPO_DE_ESPERA", "QUALIDADE_DO_SERVICO", "INSTALACOES"};
    private static final String[] NPS_IMPROVE = {"PRECO", "COMUNICACAO", "TEMPO_DE_ESPERA"};

    // Cidades brasileiras com coordenadas reais (nome, UF, lat, lng)
    private static final String[][] CITIES = {
            {"São Paulo", "SP", "-23.5505", "-46.6333"}, {"Rio de Janeiro", "RJ", "-22.9068", "-43.1729"},
            {"Belo Horizonte", "MG", "-19.9167", "-43.9345"}, {"Curitiba", "PR", "-25.4284", "-49.2733"},
            {"Porto Alegre", "RS", "-30.0346", "-51.2177"}, {"Salvador", "BA", "-12.9714", "-38.5014"},
            {"Recife", "PE", "-8.0476", "-34.8770"}, {"Fortaleza", "CE", "-3.7319", "-38.5267"},
            {"Brasília", "DF", "-15.7939", "-47.8828"}, {"Goiânia", "GO", "-16.6869", "-49.2648"},
            {"Campinas", "SP", "-22.9099", "-47.0626"}, {"Manaus", "AM", "-3.1190", "-60.0217"},
            {"Belém", "PA", "-1.4558", "-48.4902"}, {"Vitória", "ES", "-20.3155", "-40.3128"},
            {"Florianópolis", "SC", "-27.5954", "-48.5480"}, {"Natal", "RN", "-5.7945", "-35.2110"},
            {"João Pessoa", "PB", "-7.1195", "-34.8450"}, {"Maceió", "AL", "-9.6498", "-35.7089"},
            {"Cuiabá", "MT", "-15.6014", "-56.0979"}, {"Campo Grande", "MS", "-20.4697", "-54.6201"},
            {"Londrina", "PR", "-23.3045", "-51.1696"}, {"Ribeirão Preto", "SP", "-21.1775", "-47.8103"},
            {"Sorocaba", "SP", "-23.5015", "-47.4526"}, {"Uberlândia", "MG", "-18.9186", "-48.2772"},
            {"Joinville", "SC", "-26.3044", "-48.8487"}, {"Santos", "SP", "-23.9608", "-46.3336"},
            {"São Luís", "MA", "-2.5391", "-44.2829"}, {"Teresina", "PI", "-5.0892", "-42.8019"},
            {"Caxias do Sul", "RS", "-29.1678", "-51.1794"}, {"Niterói", "RJ", "-22.8833", "-43.1036"}
    };

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final AnalystRepository analystRepository;
    private final DealershipRepository dealershipRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final VehicleRepository vehicleRepository;
    private final WarrantyRepository warrantyRepository;
    private final MaintenanceAlertRepository maintenanceAlertRepository;
    private final AppointmentRepository appointmentRepository;
    private final ServiceRecordRepository serviceRecordRepository;
    private final PartUsedRepository partUsedRepository;
    private final NpsResponseRepository npsResponseRepository;
    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;
    private final RewardRepository rewardRepository;
    private final CustomerSegmentRepository customerSegmentRepository;
    private final LeadActionRepository leadActionRepository;

    private final CryptoService cryptoService;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    private final Faker faker = new Faker(new Locale("pt", "BR"), new Random(42));
    private final Random rnd = new Random(42);

    @Override
    @Transactional
    public void run(String... args) {
        if (customerRepository.count() > 0) {
            log.info("Seed ignorado: banco já contém clientes.");
            return;
        }
        log.info("Iniciando seed do dataset sintético...");

        List<ServiceType> serviceTypes = serviceTypeRepository.findAll();
        if (serviceTypes.isEmpty()) {
            serviceTypes = seedServiceTypes();
        }
        List<Reward> rewards = seedRewards();
        List<Dealership> dealerships = seedDealerships(serviceTypes);
        seedAdminAndAnalysts(dealerships);

        String encodedPassword = passwordEncoder.encode(DEFAULT_PASSWORD);
        int serviceCounter = 0;

        for (int i = 0; i < 500; i++) {
            Customer customer = createCustomer(i, encodedPassword);
            LoyaltyAccount account = loyaltyAccountRepository.save(
                    LoyaltyAccount.builder().customer(customer).balance(0).build());

            Vehicle vehicle = createVehicle(customer, i);
            WarrantyStatus wStatus = createWarranty(vehicle, i);
            maybeCreateAlerts(vehicle);

            // Define quantidade de serviços por perfil de comportamento
            int servicesForCustomer = drawServiceCount(i);
            int done = createServiceHistory(customer, vehicle, dealerships, serviceTypes, account, servicesForCustomer);
            serviceCounter += done;

            CustomerSegmentType segment = inferSegment(done, wStatus);
            seedSegment(customer, segment, done);
        }

        log.info("Seed concluído: {} clientes, {} veículos, {} serviços, {} concessionárias.",
                customerRepository.count(), vehicleRepository.count(), serviceCounter, dealershipRepository.count());
    }

    // ---------------------------------------------------------------- catálogos

    private List<ServiceType> seedServiceTypes() {
        List<ServiceType> types = List.of(
                ServiceType.builder().id("REVIEW").label("Revisão").freeWithWarranty(true).build(),
                ServiceType.builder().id("OIL_CHANGE").label("Troca de óleo").freeWithWarranty(false).build(),
                ServiceType.builder().id("WARRANTY").label("Garantia").freeWithWarranty(true).build(),
                ServiceType.builder().id("REPAIR").label("Reparo").freeWithWarranty(false).build());
        return serviceTypeRepository.saveAll(types);
    }

    private List<Reward> seedRewards() {
        List<Reward> rewards = List.of(
                Reward.builder().name("10% de desconto em troca de óleo").pointsCost(500).build(),
                Reward.builder().name("Revisão com 15% de desconto").pointsCost(800).build(),
                Reward.builder().name("Higienização interna gratuita").pointsCost(1200).build(),
                Reward.builder().name("Alinhamento e balanceamento grátis").pointsCost(1500).build(),
                Reward.builder().name("Voucher R$ 200 em peças").pointsCost(2000).build());
        return rewardRepository.saveAll(rewards);
    }

    private List<Dealership> seedDealerships(List<ServiceType> serviceTypes) {
        List<Dealership> dealerships = new ArrayList<>();
        for (String[] c : CITIES) {
            Dealership d = Dealership.builder()
                    .name("Ford " + c[0])
                    .address(faker.address().streetAddress())
                    .city(c[0]).state(c[1])
                    .zipCode(faker.numerify("#####-###"))
                    .lat(new BigDecimal(c[2])).lng(new BigDecimal(c[3]))
                    .phone(faker.numerify("+55 (##) ####-####"))
                    .openingHours("Seg-Sex 08:00-18:00; Sáb 08:00-12:00")
                    .build();
            dealerships.add(d);
        }
        dealerships = dealershipRepository.saveAll(dealerships);

        List<DealershipServiceLink> links = new ArrayList<>();
        for (Dealership d : dealerships) {
            for (ServiceType st : serviceTypes) {
                links.add(DealershipServiceLink.builder()
                        .pk(new DealershipServiceLink.Id(d.getId(), st.getId()))
                        .dealership(d).serviceType(st).active(true).build());
            }
        }
        // dealership_services usa chave composta; persistimos via EntityManager
        for (DealershipServiceLink link : links) {
            entityManager.persist(link);
        }
        return dealerships;
    }

    private void seedAdminAndAnalysts(List<Dealership> dealerships) {
        String pwd = passwordEncoder.encode(DEFAULT_PASSWORD);
        userRepository.save(User.builder()
                .email("owner@ford.com")
                .displayName("Operador Ford")
                .passwordHash(pwd)
                .role(UserRole.ADMIN)
                .build());

        for (int i = 0; i < 15; i++) {
            Dealership d = dealerships.get(i % dealerships.size());
            String analystName = faker.name().fullName();
            User u = userRepository.save(User.builder()
                    .email("analista" + i + "@ford.com")
                    .displayName(analystName)
                    .passwordHash(pwd)
                    .role(UserRole.ANALYST)
                    .build());
            analystRepository.save(Analyst.builder()
                    .user(u).dealership(d).fullName(analystName).build());
        }
    }

    // ---------------------------------------------------------------- clientes

    private Customer createCustomer(int i, String encodedPassword) {
        String name = faker.name().fullName();
        User user = userRepository.save(User.builder()
                .email("cliente" + i + "@email.com")
                .passwordHash(encodedPassword)
                .role(UserRole.CLIENT)
                .build());

        String cpf = faker.cpf().valid(false);
        return customerRepository.save(Customer.builder()
                .user(user)
                .fullName(name)
                .cpfEncrypted(cryptoService.encrypt(cpf))
                .cpfLookupHash(cryptoService.hash(cpf))
                .phone(faker.numerify("+5511#########"))
                .birthDate(LocalDate.now().minusYears(20 + rnd.nextInt(40)).minusDays(rnd.nextInt(360)))
                .lgpdConsentAt(OffsetDateTime.now().minusDays(rnd.nextInt(900)))
                .build());
    }

    private Vehicle createVehicle(Customer customer, int i) {
        String model = MODELS[rnd.nextInt(MODELS.length)];
        int year = 2017 + rnd.nextInt(9);
        return vehicleRepository.save(Vehicle.builder()
                .customer(customer)
                .model(model)
                .version(VERSIONS[rnd.nextInt(VERSIONS.length)])
                .year((short) year)
                .plate(String.format("FRD%04d", i))
                .vin(String.format("9BWZZ%012d", i))
                .currentKm(5000 + rnd.nextInt(120000))
                .build());
    }

    private WarrantyStatus createWarranty(Vehicle vehicle, int i) {
        int bucket = i % 10;
        LocalDate start;
        LocalDate end;
        WarrantyStatus status;
        if (bucket < 3) { // 30% ativas
            start = LocalDate.now().minusMonths(6 + rnd.nextInt(12));
            end = LocalDate.now().plusMonths(6 + rnd.nextInt(24));
            status = WarrantyStatus.ACTIVE;
        } else if (bucket < 7) { // 40% vencidas
            start = LocalDate.now().minusYears(4).minusMonths(rnd.nextInt(12));
            end = LocalDate.now().minusMonths(1 + rnd.nextInt(18));
            status = WarrantyStatus.EXPIRED;
        } else { // 30% vencendo em até 6 meses
            start = LocalDate.now().minusYears(3);
            end = LocalDate.now().plusDays(10 + rnd.nextInt(150));
            status = WarrantyStatus.EXPIRING_SOON;
        }
        warrantyRepository.save(Warranty.builder()
                .vehicle(vehicle).startDate(start).endDate(end).status(status).build());
        return status;
    }

    private void maybeCreateAlerts(Vehicle vehicle) {
        if (rnd.nextInt(100) < 60) {
            int nextRevisionKm = ((vehicle.getCurrentKm() / 10000) + 1) * 10000;
            maintenanceAlertRepository.save(MaintenanceAlert.builder()
                    .vehicle(vehicle)
                    .type(rnd.nextBoolean() ? MaintenanceAlertType.OIL_CHANGE : MaintenanceAlertType.REVIEW)
                    .kmThreshold(nextRevisionKm)
                    .dueDate(LocalDate.now().plusDays(rnd.nextInt(120)))
                    .build());
        }
    }

    // ---------------------------------------------------------------- serviços

    private int drawServiceCount(int i) {
        int bucket = i % 10;
        if (bucket < 3) return 4 + rnd.nextInt(5);   // fiéis: 4-8 serviços
        if (bucket < 5) return 2 + rnd.nextInt(2);   // econômicos: 2-3
        if (bucket < 7) return 1 + rnd.nextInt(2);   // esquecidos: 1-2
        if (bucket < 9) return rnd.nextInt(2);        // abandono: 0-1
        return 0;                                      // sem histórico
    }

    private int createServiceHistory(Customer customer, Vehicle vehicle, List<Dealership> dealerships,
                                     List<ServiceType> serviceTypes, LoyaltyAccount account, int count) {
        int balance = 0;
        for (int s = 0; s < count; s++) {
            Dealership dealership = dealerships.get(rnd.nextInt(dealerships.size()));
            ServiceType type = serviceTypes.get(rnd.nextInt(serviceTypes.size()));
            OffsetDateTime when = OffsetDateTime.now(ZoneOffset.UTC)
                    .minusDays(30L + rnd.nextInt(1000));

            Appointment appointment = appointmentRepository.save(Appointment.builder()
                    .customer(customer).vehicle(vehicle).dealership(dealership).serviceType(type)
                    .scheduledAt(when).status(AppointmentStatus.COMPLETED)
                    .notes(rnd.nextInt(100) < 20 ? faker.lorem().sentence() : null)
                    .build());

            BigDecimal amount = type.isFreeWithWarranty()
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(150 + rnd.nextInt(850)).setScale(2);

            ServiceRecord record = serviceRecordRepository.save(ServiceRecord.builder()
                    .appointment(appointment).vehicle(vehicle).dealership(dealership).serviceType(type)
                    .performedAt(when).totalAmount(amount)
                    .summary(serviceSummary(type))
                    .build());

            if (rnd.nextInt(100) < 50) {
                partUsedRepository.save(PartUsed.builder()
                        .service(record).partName(faker.commerce().productName())
                        .quantity(1 + rnd.nextInt(3))
                        .unitPrice(BigDecimal.valueOf(50 + rnd.nextInt(400)).setScale(2))
                        .build());
            }

            if (rnd.nextInt(100) < 60) {
                short score = (short) (rnd.nextInt(100) < 70 ? 8 + rnd.nextInt(3) : 3 + rnd.nextInt(5));
                npsResponseRepository.save(NpsResponse.builder()
                        .service(record).customer(customer).score(score)
                        .comment(rnd.nextInt(100) < 40 ? faker.lorem().sentence() : null)
                        .build());
            }

            int points = 50 + rnd.nextInt(150);
            balance += points;
            loyaltyTransactionRepository.save(LoyaltyTransaction.builder()
                    .account(account).type(LoyaltyTransactionType.EARN).points(points)
                    .sourceService(record).build());
        }
        if (balance > 0) {
            account.setBalance(balance);
            loyaltyAccountRepository.save(account);
        }
        return count;
    }

    private String serviceSummary(ServiceType type) {
        return switch (type.getId()) {
            case "REVIEW" -> "Revisão periódica realizada, sem pendências.";
            case "OIL_CHANGE" -> "Troca de óleo e filtro concluída.";
            case "WARRANTY" -> "Atendimento em garantia, item substituído.";
            default -> "Reparo executado conforme diagnóstico.";
        };
    }

    // ---------------------------------------------------------------- segmentos

    private CustomerSegmentType inferSegment(int services, WarrantyStatus warranty) {
        if (services >= 4) return CustomerSegmentType.FIEL;
        if (services == 0) return CustomerSegmentType.ABANDONO;
        if (warranty == WarrantyStatus.EXPIRING_SOON) return CustomerSegmentType.ESQUECIDO;
        return CustomerSegmentType.ECONOMICO;
    }

    private void seedSegment(Customer customer, CustomerSegmentType segment, int services) {
        int risk = switch (segment) {
            case ABANDONO -> 80 + rnd.nextInt(20);
            case ESQUECIDO -> 55 + rnd.nextInt(25);
            case ECONOMICO -> 30 + rnd.nextInt(25);
            case FIEL -> rnd.nextInt(20);
        };
        customerSegmentRepository.save(CustomerSegment.builder()
                .customer(customer)
                .segment(segment)
                .riskScore(BigDecimal.valueOf(risk).setScale(2))
                .modelVersion("seed-heuristic-v1")
                .predictedAt(OffsetDateTime.now())
                .build());
    }
}
