import {ReactNode} from 'react';
import {Navigate} from 'react-router-dom';
import {useAuth} from '../../hooks/useAuth';
import {Box, CircularProgress} from '@mui/material';

interface ProtectedRouteProps {
    children: ReactNode;
    requiredRole?: string;
    requiredRoles?: string[];
}

/**
 * Protected route component that requires authentication and optionally a specific role.
 */
export const ProtectedRoute = ({children, requiredRole, requiredRoles}: ProtectedRouteProps) => {
    const {isAuthenticated, isLoading, hasRole} = useAuth();

    if (isLoading) {
        return (
                <Box
                        display="flex"
                        justifyContent="center"
                        alignItems="center"
                        minHeight="100vh"
                >
                    <CircularProgress/>
                </Box>
        );
    }

    if (!isAuthenticated) {
        return <Navigate to="/login" replace/>;
    }

    // Check single role (for backward compatibility)
    if (requiredRole && !hasRole(requiredRole)) {
        return <Navigate to="/unauthorized" replace/>;
    }

    // Check multiple roles
    if (requiredRoles && requiredRoles.length > 0) {
        const hasAnyRole = requiredRoles.some((role) => hasRole(role));
        if (!hasAnyRole) {
            return <Navigate to="/unauthorized" replace/>;
        }
    }

    return <>{children}</>;
};

