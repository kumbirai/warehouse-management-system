import { Snackbar } from '@mui/material';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { UserForm, UserFormValues } from '../components/UserForm';
import { useCreateUser } from '../hooks/useCreateUser';
import { CreateUserRequest } from '../types/user';
import { FormPageLayout } from '../../../components/layouts';
import { getBreadcrumbs, Routes } from '../../../utils/navigationUtils';

export const UserCreatePage = () => {
  const navigate = useNavigate();
  const { createUser, isLoading, error } = useCreateUser();
  const [snackbarMessage, setSnackbarMessage] = useState<string | null>(null);

  const handleSubmit = async (values: UserFormValues) => {
    const payload: CreateUserRequest = {
      tenantId: values.tenantId,
      username: values.username,
      emailAddress: values.emailAddress,
      firstName: values.firstName || undefined,
      lastName: values.lastName || undefined,
      password: values.password,
      roles: values.roles && values.roles.length > 0 ? values.roles : undefined,
    };
    try {
      await createUser(payload);
      setSnackbarMessage('User created successfully');
      // Navigation is handled by the hook
    } catch {
      // Error is handled by hook state
    }
  };

  return (
    <>
      <FormPageLayout
        breadcrumbs={getBreadcrumbs.userCreate()}
        title="Create User"
        description="Create a new user account. The user will be created in Keycloak with the specified tenant and roles."
        error={error?.message || null}
        maxWidth="md"
      >
        <UserForm
          onSubmit={handleSubmit}
          onCancel={() => navigate(Routes.admin.users)}
          isSubmitting={isLoading}
        />
      </FormPageLayout>

      <Snackbar
        open={!!snackbarMessage}
        autoHideDuration={4000}
        onClose={() => setSnackbarMessage(null)}
        message={snackbarMessage}
      />
    </>
  );
};
