import { useState, useEffect } from 'react';
import api from '../api.js';

export default function Profile() {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  
  const [isEditing, setIsEditing] = useState(false);
  const [formData, setFormData] = useState({ firstName: '', lastName: '', age: '' });
  const [updateLoading, setUpdateLoading] = useState(false);

  useEffect(() => {
    fetchProfile();
  }, []);

  const fetchProfile = () => {
    api.get('/api/users/me')
      .then((res) => {
        setProfile(res.data);
        setFormData({
          firstName: res.data.firstName || '',
          lastName: res.data.lastName || '',
          age: res.data.age || ''
        });
      })
      .catch((err) => console.error('Failed to fetch profile', err))
      .finally(() => setLoading(false));
  };

  const handleUpdate = (e) => {
    e.preventDefault();
    setUpdateLoading(true);
    
    api.put('/api/users/me', {
      firstName: formData.firstName,
      lastName: formData.lastName,
      age: parseInt(formData.age) || null
    })
      .then((res) => {
        setProfile(res.data);
        setIsEditing(false);
        alert('Profile updated successfully!');
      })
      .catch((err) => {
        console.error('Failed to update profile', err);
        alert('Update failed. Please check your inputs!');
      })
      .finally(() => setUpdateLoading(false));
  };

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  if (loading) return <div className="p-10 text-center text-gray-600 font-medium">Loading profile...</div>;
  if (!profile) return <div className="p-10 text-center text-red-500 font-medium">Failed to load user profile.</div>;

  return (
    <div className="max-w-2xl mx-auto p-6 mt-10 bg-white rounded-xl shadow-md">
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-2xl font-bold text-gray-800">Personal Information</h2>
        {!isEditing && (
          <button 
            onClick={() => setIsEditing(true)}
            className="px-4 py-2 bg-blue-50 text-blue-600 rounded-md hover:bg-blue-100 transition font-medium text-sm"
          >
            Edit Profile
          </button>
        )}
      </div>
      
      {isEditing ? (
        <form onSubmit={handleUpdate} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-600 mb-1">First Name</label>
              <input 
                type="text" 
                name="firstName"
                value={formData.firstName}
                onChange={handleChange}
                className="w-full p-2.5 bg-white border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:outline-none"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-600 mb-1">Last Name</label>
              <input 
                type="text" 
                name="lastName"
                value={formData.lastName}
                onChange={handleChange}
                className="w-full p-2.5 bg-white border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:outline-none"
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-600 mb-1">Age</label>
            <input 
              type="number" 
              name="age"
              value={formData.age}
              onChange={handleChange}
              className="w-full p-2.5 bg-white border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:outline-none"
            />
          </div>

          <div className="pt-4 flex gap-3">
            <button 
              type="submit" 
              disabled={updateLoading}
              className="px-5 py-2.5 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition font-medium disabled:opacity-50"
            >
              {updateLoading ? 'Saving...' : 'Save Changes'}
            </button>
            <button 
              type="button" 
              onClick={() => {
                setIsEditing(false);
                setFormData({
                  firstName: profile.firstName || '',
                  lastName: profile.lastName || '',
                  age: profile.age || ''
                });
              }}
              className="px-5 py-2.5 bg-gray-100 text-gray-700 rounded-md hover:bg-gray-200 transition font-medium"
            >
              Cancel
            </button>
          </div>
        </form>
      ) : (
        <div className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-500">Full Name</label>
              <div className="mt-1 p-3 bg-gray-50 border border-gray-100 rounded-md text-gray-800 font-medium">
                {profile.firstName || profile.lastName 
                  ? `${profile.firstName || ''} ${profile.lastName || ''}`.trim() 
                  : 'N/A'}
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-500">Age</label>
              <div className="mt-1 p-3 bg-gray-50 border border-gray-100 rounded-md text-gray-800 font-medium">
                {profile.age ? profile.age : 'N/A'}
              </div>
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-500">Email Address</label>
            <div className="mt-1 p-3 bg-gray-100 border border-gray-200 rounded-md text-gray-600">
              {profile.email}
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-500">Account Role</label>
            <div className="mt-1 flex items-center">
              <span className="px-3 py-1 text-sm font-semibold text-blue-700 bg-blue-100 rounded-full border border-blue-200">
                {profile.role}
              </span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}