package com.vetclinic.user.service;

import com.vetclinic.common.AgeFormatter;
import com.vetclinic.common.ContactNormalizer;
import com.vetclinic.common.InvalidContactException;
import com.vetclinic.common.exception.FieldValidationException;
import com.vetclinic.common.exception.NotFoundException;
import com.vetclinic.common.web.EnumLabels;
import com.vetclinic.pet.repository.PetRepository;
import com.vetclinic.user.domain.Role;
import com.vetclinic.user.domain.User;
import com.vetclinic.user.repository.OwnerSpecifications;
import com.vetclinic.user.repository.UserRepository;
import com.vetclinic.user.web.OwnerDetails;
import com.vetclinic.user.web.OwnerForm;
import com.vetclinic.user.web.OwnerListItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** Owner management in the vet panel. All business rules for owners live here. */
@Service
public class OwnerService {

    public static final int PAGE_SIZE = 20;

    static final String CONTACT_REQUIRED = "Please enter an e-mail address or a phone number.";
    static final String EMAIL_TAKEN = "This e-mail address is used by another record.";
    static final String PHONE_TAKEN = "This phone number is used by another record.";

    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final ContactNormalizer contactNormalizer;
    private final AgeFormatter ageFormatter;
    private final Clock clock;

    public OwnerService(UserRepository userRepository, PetRepository petRepository,
                        ContactNormalizer contactNormalizer, AgeFormatter ageFormatter, Clock clock) {
        this.userRepository = userRepository;
        this.petRepository = petRepository;
        this.contactNormalizer = contactNormalizer;
        this.ageFormatter = ageFormatter;
        this.clock = clock;
    }

    /** Owners whose name, e-mail or phone matches {@code query} (all owners if blank), 20 per page, by name. */
    @Transactional(readOnly = true)
    public Page<OwnerListItem> search(String query, int page) {
        Specification<User> spec = OwnerSpecifications.isOwner();
        if (query != null && !query.isBlank()) {
            spec = spec.and(OwnerSpecifications.matches(query, phoneFragment(query)));
        }
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by("fullName", "id"));
        return userRepository.findAll(spec, pageRequest)
                .map(u -> new OwnerListItem(u.getId(), u.getFullName(), u.getEmail(), u.getPhone(), u.isActive()));
    }

    @Transactional(readOnly = true)
    public OwnerDetails getDetails(Long id) {
        User owner = findOwner(id);
        var pets = petRepository.findAllByOwnerIdOrderByName(id).stream()
                .map(p -> new OwnerDetails.OwnerPet(p.getId(), p.getName(), EnumLabels.of(p.getSpecies()),
                        p.getBreed(), ageFormatter.format(p.getBirthDate())))
                .toList();
        var consentDate = owner.getKvkkConsentAt() == null ? null
                : owner.getKvkkConsentAt().atZoneSameInstant(clock.getZone()).toLocalDate();
        return new OwnerDetails(owner.getId(), owner.getFullName(), owner.getEmail(), owner.getPhone(),
                owner.isActive(), consentDate, pets);
    }

    @Transactional(readOnly = true)
    public OwnerForm getForm(Long id) {
        User owner = findOwner(id);
        OwnerForm form = new OwnerForm();
        form.setFullName(owner.getFullName());
        form.setEmail(owner.getEmail());
        form.setPhone(owner.getPhone());
        form.setKvkkConsent(owner.getKvkkConsentAt() != null);
        return form;
    }

    /** Creates an active OWNER with normalized contacts; returns the new id. */
    @Transactional
    public Long create(OwnerForm form) {
        User owner = new User();
        owner.setRole(Role.OWNER);
        owner.setActive(true);
        apply(form, owner, null);
        return userRepository.save(owner).getId();
    }

    @Transactional
    public void update(Long id, OwnerForm form) {
        User owner = findOwner(id);
        apply(form, owner, id);
    }

    /** Owners are never deleted; an inactive owner cannot log in. */
    @Transactional
    public void deactivate(Long id) {
        findOwner(id).setActive(false);
    }

    @Transactional
    public void reactivate(Long id) {
        findOwner(id).setActive(true);
    }

    private User findOwner(Long id) {
        return userRepository.findById(id)
                .filter(u -> u.getRole() == Role.OWNER)
                .orElseThrow(() -> new NotFoundException("Owner not found: " + id));
    }

    /**
     * Validates and copies the form onto the entity. {@code ownId} is the owner being edited (null when
     * creating), so its own e-mail/phone does not count as a duplicate.
     */
    private void apply(OwnerForm form, User owner, Long ownId) {
        Map<String, String> errors = new LinkedHashMap<>();
        String email = normalizeOrCollect(form.getEmail(), true, errors);
        String phone = normalizeOrCollect(form.getPhone(), false, errors);

        if (errors.isEmpty() && email == null && phone == null) {
            errors.put("email", CONTACT_REQUIRED);
            errors.put("phone", CONTACT_REQUIRED);
        }
        if (email != null && (ownId == null ? userRepository.existsByEmail(email)
                : userRepository.existsByEmailAndIdNot(email, ownId))) {
            errors.put("email", EMAIL_TAKEN);
        }
        if (phone != null && (ownId == null ? userRepository.existsByPhone(phone)
                : userRepository.existsByPhoneAndIdNot(phone, ownId))) {
            errors.put("phone", PHONE_TAKEN);
        }
        if (!form.isKvkkConsent()) {
            errors.put("kvkkConsent", "The owner's KVKK consent is required.");
        }
        if (!errors.isEmpty()) {
            throw new FieldValidationException(errors);
        }

        owner.setFullName(form.getFullName().trim());
        owner.setEmail(email);
        owner.setPhone(phone);
        if (owner.getKvkkConsentAt() == null) {
            owner.setKvkkConsentAt(OffsetDateTime.now(clock));
        }
    }

    /** Blank → null; invalid → error collected under the field name. */
    private String normalizeOrCollect(String raw, boolean isEmail, Map<String, String> errors) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return isEmail ? contactNormalizer.normalizeEmail(raw) : contactNormalizer.normalizePhone(raw);
        } catch (InvalidContactException e) {
            errors.put(isEmail ? "email" : "phone", e.getMessage());
            return null;
        }
    }

    /**
     * Phone part of the search: the normalized number if the text is a full phone ("0532 123 45 67"),
     * otherwise its digits without a leading 0 ("0532" → "532") when there are at least 3, otherwise null.
     */
    private String phoneFragment(String query) {
        try {
            return contactNormalizer.normalizePhone(query);
        } catch (InvalidContactException e) {
            String digits = query.replaceAll("[\\s\\-()+]", "");
            if (!digits.matches("\\d{3,}")) {
                return null;
            }
            return digits.startsWith("0") ? digits.substring(1) : digits;
        }
    }
}
