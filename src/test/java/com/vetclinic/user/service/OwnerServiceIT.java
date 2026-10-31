package com.vetclinic.user.service;

import com.vetclinic.AbstractIntegrationTest;
import com.vetclinic.user.domain.Role;
import com.vetclinic.user.domain.User;
import com.vetclinic.user.repository.UserRepository;
import com.vetclinic.user.web.OwnerForm;
import com.vetclinic.user.web.OwnerListItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/** Owner search and create against the real database (Specifications, normalization). */
@Transactional
class OwnerServiceIT extends AbstractIntegrationTest {

    @Autowired
    private OwnerService ownerService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        save("Ayşe Demir", "ayse.demir@example.com", null, Role.OWNER);
        save("Zeynep Arslan", null, "+905334445566", Role.OWNER);
        save("Burak Aydın", "burak@example.com", "+905469876543", Role.OWNER);
        save("Dr. Elif Yılmaz", "elif@example.com", "+905321112233", Role.VET);
    }

    @Test
    void searchMatchesNameEmailAndPhoneAndNeverReturnsVets() {
        assertThat(names(ownerService.search(null, 0))).containsExactly("Ayşe Demir", "Burak Aydın", "Zeynep Arslan");
        assertThat(names(ownerService.search("zEyNeP", 0))).containsExactly("Zeynep Arslan");
        assertThat(names(ownerService.search("AYSE.DEMIR@", 0))).containsExactly("Ayşe Demir");
        // full phone in any accepted format is normalized
        assertThat(names(ownerService.search("0533 444 55 66", 0))).containsExactly("Zeynep Arslan");
        assertThat(names(ownerService.search("90 546 987 65 43", 0))).containsExactly("Burak Aydın");
        // partial digits, leading 0 ignored
        assertThat(names(ownerService.search("0546", 0))).containsExactly("Burak Aydın");
        // the vet's phone does not show up
        assertThat(names(ownerService.search("0532 111 22 33", 0))).isEmpty();
        // LIKE wildcards are matched literally
        assertThat(names(ownerService.search("%", 0))).isEmpty();
    }

    @Test
    void createdOwnerIsStoredNormalized() {
        OwnerForm form = new OwnerForm();
        form.setFullName("Ali Veli");
        form.setEmail("Ali.Veli@Example.com");
        form.setPhone("0532 123 45 67");
        form.setKvkkConsent(true);

        Long id = ownerService.create(form);

        User saved = userRepository.findById(id).orElseThrow();
        assertThat(saved.getPhone()).isEqualTo("+905321234567");
        assertThat(saved.getEmail()).isEqualTo("ali.veli@example.com");
        assertThat(saved.getKvkkConsentAt()).isNotNull();
        assertThat(saved.isActive()).isTrue();
    }

    private void save(String name, String email, String phone, Role role) {
        User user = new User();
        user.setFullName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setRole(role);
        userRepository.save(user);
    }

    private static java.util.List<String> names(Page<OwnerListItem> page) {
        return page.map(OwnerListItem::fullName).getContent();
    }
}
