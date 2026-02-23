package com.openfolio.export.dto;

/**
 * Options the user can toggle when generating a PDF resume.
 *
 * @param aiRewriteDescriptions  rewrite raw GitHub project descriptions with AI
 * @param includePhoto           embed the user's profile photo in the header
 * @param photoUrl               custom photo URL (null → use GitHub avatar_url)
 * @param includePhone           include phone number in contact line
 * @param phone                  phone number text
 * @param includeLinkedIn        include LinkedIn profile link
 * @param linkedIn               LinkedIn username or full URL
 * @param includeWebsite         include personal website link
 * @param website                personal website URL
 */
public record ExportOptions(
        boolean aiRewriteDescriptions,
        boolean includePhoto,
        String  photoUrl,
        boolean includePhone,
        String  phone,
        boolean includeLinkedIn,
        String  linkedIn,
        boolean includeWebsite,
        String  website
) {
    /** Sensible defaults — everything off, no extra fields. */
    public static ExportOptions defaults() {
        return new ExportOptions(false, false, null, false, null, false, null, false, null);
    }

    /** Builder-style factory from individual query params. */
    public static ExportOptions of(boolean aiRewrite, boolean photo, String photoUrl,
                                    boolean phone, String phoneVal,
                                    boolean linkedIn, String linkedInVal,
                                    boolean website, String websiteVal) {
        return new ExportOptions(aiRewrite, photo, photoUrl, phone, phoneVal,
                linkedIn, linkedInVal, website, websiteVal);
    }
}
