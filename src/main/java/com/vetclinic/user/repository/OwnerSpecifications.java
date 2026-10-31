package com.vetclinic.user.repository;

import com.vetclinic.user.domain.Role;
import com.vetclinic.user.domain.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Query building blocks for the owner list in the vet panel. */
public final class OwnerSpecifications {

    private OwnerSpecifications() {
    }

    public static Specification<User> isOwner() {
        return (root, query, cb) -> cb.equal(root.get("role"), Role.OWNER);
    }

    /**
     * Matches the name (case-insensitive) or the e-mail (contains), or the phone (contains {@code phoneFragment}).
     * Blank text and a null phone fragment are skipped; if every part is skipped, nothing matches.
     *
     * @param text          the raw search text, used for name and e-mail
     * @param phoneFragment a normalized phone or a digit fragment of one, or null
     */
    public static Specification<User> matches(String text, String phoneFragment) {
        return (root, query, cb) -> {
            List<Predicate> any = new ArrayList<>();
            String pattern = "%" + escapeLike(text.trim().toLowerCase(Locale.ROOT)) + "%";
            any.add(cb.like(cb.lower(root.get("fullName")), pattern, '\\'));
            any.add(cb.like(root.get("email"), pattern, '\\'));
            if (phoneFragment != null) {
                any.add(cb.like(root.get("phone"), "%" + escapeLike(phoneFragment) + "%", '\\'));
            }
            return cb.or(any.toArray(Predicate[]::new));
        };
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
