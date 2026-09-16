package ir.ac.kntu.domain.user;

import ir.ac.kntu.exception.ValidationException;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Customer support specialist restricted to assigned functional sections.
 */
public class SupportUser extends User {
    private final String username;
    private final Set<SupportSection> sections;

    public SupportUser(String firstName, String lastName, String username, String password) {
        super(firstName, lastName, password);
        if (username == null || username.trim().isEmpty()) {
            throw new ValidationException("Support username cannot be empty.");
        }
        this.username = username.trim();
        this.sections = EnumSet.noneOf(SupportSection.class);
    }

    public String getUsername() {
        return username;
    }

    public Set<SupportSection> getSections() {
        return Collections.unmodifiableSet(sections);
    }

    public void addSection(SupportSection section) {
        sections.add(Objects.requireNonNull(section, "Section cannot be null."));
    }

    public void removeSection(SupportSection section) {
        sections.remove(section);
    }

    public boolean hasSection(SupportSection section) {
        return sections.contains(section);
    }

    public void setSections(Set<SupportSection> newSections) {
        sections.clear();
        if (newSections != null) {
            sections.addAll(newSections);
        }
    }
}