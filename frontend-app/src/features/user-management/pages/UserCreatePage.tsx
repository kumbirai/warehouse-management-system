import { Alert, Breadcrumbs, Container, Link, Snackbar, Stack, Typography } from '@mui/material';
import { useState } from 'react';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import { Header } from '../../../components/layout/Header';
import { UserForm, UserFormValues } from '../components/UserForm';
import { useCreateUser } from '../hooks/useCreateUser';
import { CreateUserRequest } from '../types/user';

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
      <Header />
      <Container maxWidth="md" sx={{ py: 4 }}>
        <Breadcrumbs sx={{ mb: 2 }}>
          <Link component={RouterLink} to="/dashboard">
            Dashboard
          </Link>
          <Link component={RouterLink} to="/admin/users">
            Users
          </Link>
          <Typography color="text.primary">Create User</Typography>
        </Breadcrumbs>
        <Stack spacing={2} mb={3}>
          <Typography variant="h4">Create User</Typography>
          <Typography variant="body1" color="text.secondary">
            Create a new user account. The user will be created in Keycloak with the specified
            tenant and roles.
          </Typography>
        </Stack>

        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error.message}
          </Alert>
        )}

        <UserForm
          onSubmit={handleSubmit}
          onCancel={() => navigate('/admin/users')}
          isSubmitting={isLoading}
        />
      </Container>

      <Snackbar
        open={!!snackbarMessage}
        autoHideDuration={4000}
        onClose={() => setSnackbarMessage(null)}
        message={snackbarMessage}
      />
    </>
  );
};
