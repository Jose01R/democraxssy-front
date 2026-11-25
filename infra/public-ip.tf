# Source from https://registry.terraform.io/providers/oracle/oci/latest/docs/resources/core_public_ip

# IP privada principal de la VM, para asociarle la IP pública reservada
data "oci_core_vnic_attachments" "backend" {
  compartment_id = var.compartment_ocid
  instance_id    = oci_core_instance.backend_instance.id
}

data "oci_core_private_ips" "backend" {
  vnic_id = data.oci_core_vnic_attachments.backend.vnic_attachments[0].vnic_id
}

# IP reservada: no cambia aunque la VM se detenga o se recree
resource "oci_core_public_ip" "backend_ip" {
  compartment_id = var.compartment_ocid
  lifetime       = "RESERVED"
  display_name   = "ip-democraxssy-back"
  private_ip_id  = data.oci_core_private_ips.backend.private_ips[0].id

  freeform_tags = {
    proyecto = "democraxssy"
  }
}
