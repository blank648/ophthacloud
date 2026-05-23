-- V23__cross_module_fks.sql
-- Cross-module foreign key constraints

-- consultation_sections → clinical_templates
ALTER TABLE consultation_sections
    ADD CONSTRAINT fk_sections_template
    FOREIGN KEY (template_id) REFERENCES clinical_templates(id);
