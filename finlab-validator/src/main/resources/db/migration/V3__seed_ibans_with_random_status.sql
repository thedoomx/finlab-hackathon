-- Seed 1 million valid IBANs with random status values
-- Only run the generation when the table is empty
DO
$$
DECLARE
    current_count BIGINT;
    total_to_generate INTEGER := 1000000;
    i INTEGER;
    raw_digits BIGINT;
    rand_digits TEXT;
    bban TEXT;
    rearranged TEXT;
    rem INTEGER;
    ch TEXT;
    j INTEGER;
    char_code INTEGER;
    digit_char TEXT;
    check_val INTEGER;
    check_str TEXT;
    full_iban TEXT;
    status_choices TEXT[] := ARRAY['ALLOW','REVIEW','BLOCK'];
    chosen_status TEXT;
BEGIN
    SELECT COUNT(*) INTO current_count FROM hackathon.iban;

    IF current_count > 0 THEN
        RAISE NOTICE 'hackathon.iban is not empty (count = %). Skipping IBAN generation.', current_count;
        RETURN;
    END IF;

    RAISE NOTICE 'hackathon.iban is empty. Generating % IBANs...', total_to_generate;

    FOR i IN 1..total_to_generate LOOP
        -- Generate 14 random digits (leading zeros allowed)
        -- Use random() * 10^14, convert to bigint then format to exactly 14 digits
        raw_digits := trunc(random() * 100000000000000)::bigint; -- 10^14
        rand_digits := lpad(raw_digits::text, 14, '0');

        -- BBAN = bank id letters + 14 digits. Mask requires literal 'BANK'
        bban := 'BANK' || rand_digits;  -- length 18 (4 letters + 14 digits)

        /*
           IBAN check digit calculation (per ISO 7064 mod 97-10):
           1) Form string: BBAN + country code letters + "00"  (we use 'BG' here)
           2) Replace each letter with two digits: A=10, B=11, ..., Z=35
           3) Compute integer value mod 97 (done streaming to avoid big ints)
           4) check = 98 - (mod97 result)
           5) IBAN = country + check(2 digits) + BBAN
        */

        rearranged := bban || 'BG' || '00';

        -- streaming remainder calculation: process each character and update remainder
        rem := 0;
        j := 1;
        WHILE j <= char_length(rearranged) LOOP
            ch := substr(rearranged, j, 1);

            IF ch >= '0' AND ch <= '9' THEN
                -- a single decimal digit; process it
                rem := ((rem * 10) + (ascii(ch) - ascii('0'))) % 97;
            ELSE
                -- letter: convert A..Z -> 10..35, then process each digit of that number
                char_code := ascii(upper(ch));
                -- 'A' = 65 -> 10 ; so formula: ascii - 55
                IF char_code >= 65 AND char_code <= 90 THEN
                    digit_char := (char_code - 55)::text; -- e.g. '11' for 'B'
                    -- process each digit of digit_char
                    FOR k IN 1..char_length(digit_char) LOOP
                        rem := ((rem * 10) + (ascii(substr(digit_char,k,1)) - ascii('0'))) % 97;
                    END LOOP;
                ELSE
                    -- Shouldn't happen, but skip defensively
                END IF;
            END IF;

            j := j + 1;
        END LOOP;

        check_val := 98 - rem;
        IF check_val = 98 THEN
            check_val := 0;
        END IF;
        check_str := lpad(check_val::text, 2, '0');

        full_iban := 'BG' || check_str || bban;  -- final IBAN

        -- choose random status
        chosen_status := status_choices[ (floor(random() * 3)::int) + 1 ];

        -- Insert into table; if duplicate (very unlikely) skip
        INSERT INTO hackathon.iban (iban, status)
        VALUES (full_iban, chosen_status::hackathon.iban_status)
        ON CONFLICT DO NOTHING;

        -- Optional: periodic progress notice every 100k rows to avoid silent long run
        IF (i % 100000) = 0 THEN
            RAISE NOTICE 'Inserted % IBANs so far...', i;
        END IF;
    END LOOP;

    RAISE NOTICE 'Completed IBAN generation.';
END;
$$;
