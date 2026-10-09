export type UserRole = 'SUPER_ADMIN' | 'ADMIN' | 'MODERATOR' | 'CONTENT_MANAGER' | 'BUSINESS_OWNER' | 'USER';

export interface Profile {
  id: string;
  auth_user_id: string;
  full_name: string;
  email?: string;
  phone?: string;
  is_verified: boolean;
  status: 'ACTIVE' | 'SUSPENDED' | 'DELETED';
  created_at: string;
}

export interface Notice {
  id: string;
  title_gu: string;
  title_en: string;
  description_gu?: string;
  description_en?: string;
  status: 'DRAFT' | 'PENDING' | 'PUBLISHED' | 'EXPIRED' | 'ARCHIVED';
  is_pinned: boolean;
  publish_at?: string;
  created_at: string;
}

export interface VillageEvent {
  id: string;
  title_gu: string;
  title_en: string;
  description_gu?: string;
  description_en?: string;
  start_at: string;
  end_at?: string;
  location_gu?: string;
  location_en?: string;
  status: 'DRAFT' | 'PUBLISHED' | 'CANCELLED' | 'COMPLETED';
}

export interface Business {
  id: string;
  name_gu: string;
  name_en: string;
  category_name_gu?: string;
  category_name_en?: string;
  phone?: string;
  whatsapp?: string;
  address_gu?: string;
  address_en?: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'SUSPENDED';
  is_verified: boolean;
  created_at: string;
}

export interface Job {
  id: string;
  title_gu: string;
  title_en: string;
  company_name?: string;
  location_gu?: string;
  location_en?: string;
  salary_min?: number;
  salary_max?: number;
  status: 'PENDING' | 'PUBLISHED' | 'EXPIRED' | 'CLOSED';
}

// CRITICAL: Offering / ચઢાવો is NOT an auction
export interface Offering {
  id: string;
  title_gu: string;
  title_en: string;
  description_gu?: string;
  description_en?: string;
  amount?: number;
  quantity?: number;
  location_gu?: string;
  location_en?: string;
  status: 'DRAFT' | 'PENDING_APPROVAL' | 'APPROVED' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED' | 'EXPIRED' | 'REJECTED';
  created_at: string;
  bid_call_count: number;
  winning_bidder_name: string | null;
  winning_bid_amount: number | null;
}

export interface OfferingBid {
  id: string;
  offering_id: string;
  bidder_name: string;
  bidder_phone?: string;
  bidder_profile_id?: string | null;
  amount: number;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  created_at: string;
}

export interface Complaint {
  id: string;
  user_id: string;
  title: string;
  description: string;
  status: 'SUBMITTED' | 'UNDER_REVIEW' | 'ASSIGNED' | 'IN_PROGRESS' | 'RESOLVED' | 'REJECTED' | 'CLOSED';
  priority: 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT';
  resolution_note?: string;
  created_at: string;
}

export interface EmergencyContact {
  id: string;
  name_gu: string;
  name_en: string;
  organization?: string;
  phone: string;
  alternate_phone?: string;
  category: 'AMBULANCE' | 'POLICE' | 'HOSPITAL' | 'FIRE' | 'GOVERNMENT' | 'HELPLINE';
  display_order: number;
  is_active: boolean;
}

export interface AuditLog {
  id: string;
  user_id?: string;
  action: string;
  entity_type?: string;
  entity_id?: string;
  old_data?: any;
  new_data?: any;
  created_at: string;
}
