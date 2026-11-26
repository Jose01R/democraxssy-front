# Source from https://registry.terraform.io/providers/oracle/oci/latest/docs/resources/core_instance

data "oci_core_images" "ol9" {
  compartment_id           = var.compartment_ocid
  operating_system         = "Oracle Linux"
  operating_system_version = "9"
  shape                    = "VM.Standard.E2.1.Micro"
  sort_by                  = "TIMECREATED"
  sort_order               = "DESC"
}

resource "oci_core_instance" "backend_instance" {
  # Required
  availability_domain = data.oci_identity_availability_domains.ads.availability_domains[var.ad_index].name
  compartment_id      = var.compartment_ocid
  shape               = "VM.Standard.E2.1.Micro"
  source_details {
    source_id   = data.oci_core_images.ol9.images[0].id
    source_type = "image"
  }

  # Optional
  display_name = "democraxssy-back-e2"
  create_vnic_details {
    assign_public_ip = false
    subnet_id        = var.subnet_ocid
    hostname_label   = "democraxssy-back"
  }
  metadata = {
    ssh_authorized_keys = file(pathexpand(var.ssh_public_key_path))
    user_data = base64encode(templatefile("${path.module}/cloud-init.yaml", {
      api_domain   = var.api_domain
      jvm_max_heap = var.jvm_max_heap
    }))
  }
  preserve_boot_volume = false

  freeform_tags = {
    proyecto = "democraxssy"
    rol      = "backend"
  }

  lifecycle {
    ignore_changes = [source_details[0].source_id]
  }
}

data "oci_core_image" "instance_image" {
  image_id = oci_core_instance.backend_instance.source_details[0].source_id
}
